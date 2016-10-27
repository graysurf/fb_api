package com.pubgame.itd.actor

import java.time.format.DateTimeFormatter
import java.time.{ LocalDate, LocalDateTime }

import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, Props, Status, Terminated }
import akka.dispatch.MessageDispatcher
import akka.pattern._
import akka.util.Timeout
import com.pubgame.itd.actor.GameInfoActor._
import com.pubgame.itd.actor.fb.FbInsightActor.{ FbInsightActorMessage, SubmitInsightJob }
import com.pubgame.itd.actor.fb.FbRequestActor._
import com.pubgame.itd.actor.fb._
import com.pubgame.itd.actor.migrate.gd.GdActor
import com.pubgame.itd.actor.migrate.pg.PgActor
import com.pubgame.itd.actor.migrate.{ DB, Migrate }
import com.pubgame.itd.http.FbHttpClientEndpoint

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.control.NonFatal

class GameInfoActor(
    pgSource: DB,
    gdSource: DB,
    target: DB
) extends Actor with ActorLogging {

  // implicits
  import context.{ dispatcher, system }

  implicit val timeout: Timeout = 15 minutes
  val maxRequestRetryCount: Int = 5

  // fields
  @transient
  private[this] var endpoint: FbHttpClientEndpoint = _
  @transient
  private[this] var fbRequestActor: ActorRef = _
  @transient
  private[this] var fbInsightActor: ActorRef = _
  @transient
  private[this] var fbShutdownAdActor: ActorRef = _
  @transient
  private[this] var restartTimes: Int = 0

  private[this] val pgActor = context.actorOf(Props(classOf[PgActor], pgSource, target), "pg")

  private[this] val gdActor = context.actorOf(Props(classOf[GdActor], gdSource, target), "gd")

  def setup() = {

    log.info("start setup GameInfoActor")

    context.become(init)

    if (endpoint != null) {
      endpoint.shutdown
    }

    context.children.foreach {
      ref ⇒
        if (!ref.eq(pgActor) && !ref.eq(gdActor))
          context.stop(ref)
      //ref ! PoisonPill
    }

    endpoint = new FbHttpClientEndpoint()
    val createTime = {
      val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd'D'HH:mm")) + s"-$restartTimes"
      restartTimes += 1
      time
    }
    fbRequestActor = context.actorOf(Props(classOf[FbRequestActor], target, timeout, endpoint, maxRequestRetryCount), s"fb_request_$createTime")
    fbInsightActor = context.actorOf(Props(classOf[FbInsightActor], fbRequestActor, target, timeout, endpoint, maxRequestRetryCount).withDispatcher("dispatcher.insight"), s"fb_insight_$createTime")
    fbShutdownAdActor = context.actorOf(Props(classOf[FbShutdownAdActor], fbRequestActor, target, timeout, endpoint, maxRequestRetryCount), s"fb_shutdown_$createTime")

    context.watch(fbRequestActor)
    context.watch(fbInsightActor)
    context.watch(fbShutdownAdActor)

    system.scheduler.scheduleOnce(60 seconds) {
      context.become(receive)
    }

  }

  override def preStart(): Unit = {
    super.preStart()
    setup()
    val sPgCheck = pgSource.db.run {
      import pgSource.driver.api._
      sql"SELECT VERSION();".as[String].head
    }
      .map(v ⇒ log.info(s"source PG DB version $v connection started"))

    val sGdCheck = gdSource.db.run {
      import pgSource.driver.api._
      sql"SELECT VERSION();".as[String].head
    }
      .map(v ⇒ log.info(s"source GD DB version $v connection started"))

    val tCheck = target.db.run {
      import target.driver.api._
      sql"SHOW SERVER_VERSION".as[String].head
    }
      .map(v ⇒ log.info(s"target DB version $v connection started"))

    val check = (for { _ ← sPgCheck; _ ← sGdCheck; _ ← tCheck } yield ()).recover {
      case NonFatal(t) ⇒
        log.error(t, "db connection error")
        throw t
    }
    Await.ready(check, Duration.Inf)

    //    system.scheduler.schedule(30 seconds, 10 minutes) {
    //      val isAlive = fbRequestActor.ask(IsAlive)(3 minutes)
    //      isAlive.onComplete {
    //        case scala.util.Success(a) ⇒
    //          log.info("fbRequestActor is alive")
    //        case scala.util.Failure(t) ⇒
    //          log.warning("fbRequestActor was dead")
    //          setup()
    //      }
    //
    //    }

    //    system.scheduler.schedule(3 hours, 170 minutes) {
    //      setup()
    //    }

  }

  override def postStop(): Unit = {
    val sShutdown = pgSource.db.shutdown
    val tShutdown = target.db.shutdown

    val f = for {
      _ ← sShutdown
      _ ← tShutdown
    } yield log.info("database shutdown complete")

    Await.ready(f, Duration.Inf)
    super.postStop()
  }

  def updateAccountActor() = {
    fbRequestActor ? UpdateAccountAcotr
  }

  def updateFbInfo() = {
    log.info("updateFbInfo start")
    Future.sequence(
      Seq(
        fbRequestActor ? GetCampaignInfo,
        fbRequestActor ? GetAdSetInfo,
        fbRequestActor ? GetAudienceInfo,
        // fbRequestActor ? GetAccountInfo,
        fbRequestActor ? GetAdInfo,
        fbRequestActor ? GetCreativeInfo
      )
    )
  }

  def refreshMonitorMview = {
    fbRequestActor ? RefreshMonitorTables
  }

  def refreshAutoADMview = {
    fbRequestActor ? RefreshAutoAdTables
  }

  override def receive: Receive = {

    case SetupSystem ⇒
      setup()

    // insight
    case message: FbInsightActorMessage ⇒
      fbInsightActor.forward(message)

    case SubmitInsightDailyJob ⇒
      val today = LocalDate.now()
      fbInsightActor.forward(SubmitInsightJob(today.minusDays(1), today))

    case SubmitInsightMonthJob ⇒
      val today = LocalDate.now()
      fbInsightActor.forward(SubmitInsightJob(today.minusMonths(1), today))

    case ShutdownCheck ⇒
      fbShutdownAdActor.forward(ShutdownCheck)

    // All FB Job
    case UpdateAccount ⇒
      updateAccountActor().map(_ ⇒ FetchFbJobDone).pipeTo(sender)

    case job: Job ⇒
      log.info(s"start $job")
      fbRequestActor ! job

    case m @ RefreshMonitorMview(errorCounts) ⇒
      refreshMonitorMview
        .map(_ ⇒ FetchFbJobDone)
    //        .recoverWith {
    //          case NonFatal(t) if errorCounts < maxRequestRetryCount =>
    //            val nextErrorCount = errorCounts + 1
    //            log.warning("{} error, retry {} time", m, nextErrorCount)
    //            self ? RefreshMonitorMview(nextErrorCount)
    //        }
    //        .pipeTo(sender())

    case m @ RefreshAutoAdMview(errorCounts) ⇒
      refreshAutoADMview
        .map(_ ⇒ RefreshAutoAdMviewStart)
    //        .recoverWith {
    //          case NonFatal(t) if errorCounts < maxRequestRetryCount =>
    //            val nextErrorCount = errorCounts + 1
    //            log.warning("{} error, retry {} time", m, nextErrorCount)
    //            self ? RefreshAutoAdMview(nextErrorCount)
    //        }
    //        .pipeTo(sender())

    //Pubagme Data
    case MigratePgDb ⇒
      pgActor.forward(Migrate)
      sender() ! MigrateDbStart

    //Game Dreamer Data
    case MigrateGdDb ⇒
      gdActor.forward(Migrate)
      sender() ! MigrateDbStart

    case Terminated(ref) ⇒
      log.info("{} Terminated", ref)
      context.unwatch(ref)

    case Status.Failure(t) ⇒
      log.error(t, s"Exception from $sender")

    case any ⇒
      log.warning("receive unknown message {} from {}", any, sender)
  }

  def init: Receive = {
    case SetupSystem ⇒
      log.warning("ignore message {} during initializing", SetupSystem)
    case any ⇒
      system.scheduler.scheduleOnce(15 seconds) {
        self.forward(any)
      }
  }

}

object GameInfoActor {

  class FbConnectionException(cause: Throwable) extends Exception

  case object SetupSystem

  // jobs
  case object UpdateAccount

  case object MigratePgDb

  case object MigrateGdDb

  case object SubmitInsightDailyJob

  case object SubmitInsightMonthJob

  case object ShutdownCheck

  case object FetchFbJobDone

  case object MigrateDbStart

  case class RefreshMonitorMview(errorCounts: Int = 0)

  case class RefreshAutoAdMview(errorCounts: Int = 0)

  case class RefreshOldMview(errorCounts: Int = 0)

  case object RefreshAutoAdMviewStart

  case object IsAlive

}