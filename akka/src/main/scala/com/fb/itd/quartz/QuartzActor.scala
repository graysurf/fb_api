package com.pubgame.itd.quartz

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.pattern._
import akka.util.Timeout
import com.pubgame.itd.actor.GameInfoActor._
import com.pubgame.itd.actor.fb.FbRequestActor._
import com.pubgame.itd.actor.migrate.DB
import com.pubgame.itd.db.Tables

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class QuartzActor(target: ActorRef, production: DB) extends Actor with ActorLogging {

  import context.dispatcher
  import production.db
  import production.driver.api._

  implicit val timeout: Timeout = 15 minutes

  val jobOf = Seq(

    GetCampaignInfo,
    GetAdSetInfo,
    GetAudienceInfo,
    GetAccountInfo,
    GetAdInfo,
    GetCreativeInfo,

    UpdateAccount,

    MigratePgDb,
    MigrateGdDb,

    SubmitInsightDailyJob,
    SubmitInsightMonthJob,

    ShutdownCheck,

    RefreshAutoAdMview()

  //    RefreshMonitorMview(),
  //    RefreshOldMview()
  )
    .map(msg ⇒ msg.getClass.getSimpleName.stripSuffix("$").toLowerCase() → msg)
    .toMap

  def getSchedule: Future[Seq[ScheduleData]] = {
    log.info("check schedule from db")

    val scheduleType = db.run(Tables.scheduleType.result)
    val schedule = db.run(Tables.schedule.result)

    scheduleType.onFailure { case t ⇒ log.error(t, "get scheduleType table error") }
    schedule.onFailure { case t ⇒ log.error(t, "get schedule table error") }

    for {
      typeRow ← scheduleType
      scheduleRow ← schedule
    } yield {
      val cron = typeRow.map { r ⇒ r.name → CronData(r.name, r.cron, r.range) }.toMap
      scheduleRow.flatMap {
        row ⇒
          row.cron.split(';').map(_.trim).map {
            c ⇒
              val job = cron.get(c)
              require(job.isDefined, s"'$c' trigger does not exist on schedule_type table.")
              ScheduleData(row.name, row.status.toLowerCase, job.get)
          }
      }
    }
  }

  def scheduleRun(s: Seq[ScheduleData]): Unit = {
    val scheduleOn: Seq[ScheduleData] = s.filter(_.status == "on")
    val scheduleOff: Seq[ScheduleData] = s.filter(_.status == "off")

    log.info(s"disable schedule: ${scheduleOff.groupBy(_.name).size} db rows.")
    scheduleOff.foreach(s ⇒ log.info(s.toString))

    log.info(s"enable schedule: ${scheduleOn.groupBy(_.name).size} db rows.")

    scheduleOn.foreach {
      case d @ ScheduleData(name, status, cron) if jobOf.isDefinedAt(name.toLowerCase) ⇒
        log.info(s"schedule: ${d.toString}")

        QuartzScheduler.newJob(s"$name-${cron.name}", cron.cron) {
          target ! jobOf(name.toLowerCase)
        }
      case d @ ScheduleData(name, _, _) ⇒
        log.warning("schedule name {} not found", name)
    }

  }

  override def receive: Receive = {
    case RunQuartzMessage ⇒
      getSchedule.map(scheduleRun)
    case any ⇒
    //log.warning("receive unknown message {} from {}", any, sender)
  }

}

