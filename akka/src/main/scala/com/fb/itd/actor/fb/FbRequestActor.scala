package com.pubgame.itd.actor.fb

import java.time.LocalDate

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef, Props, Terminated }
import akka.pattern._
import akka.util.Timeout
import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.itd.actor.GameInfoActor.{ FbConnectionException, IsAlive, SetupSystem, ShutdownCheck }
import com.pubgame.itd.actor.fb.FbInsightActor.InsightAsyncJob
import com.pubgame.itd.actor.fb.FbRequestActor._
import com.pubgame.itd.actor.migrate.DB
import com.pubgame.itd.db.Tables
import com.pubgame.itd.db.json.adset.{ AdSetRow, TargetingSpecHelper }
import com.pubgame.itd.db.json.{ AccountJsonRow, AdJsonRow, CreativeRow, JsonRow }
import com.pubgame.itd.http.FbHttpClientEndpoint
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.{ HanyuPinyinCaseType, HanyuPinyinOutputFormat, HanyuPinyinToneType, HanyuPinyinVCharType }

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.control.NonFatal

class FbRequestActor(
    override val production: DB,
    override protected[this] implicit val timeout: Timeout,
    override protected[this] val endpoint: FbHttpClientEndpoint,
    override protected[this] val maxRequestRetryCount: Int
) extends Actor with ActorLogging with FbRequestHelper with TargetingSpecHelper {
  // implicits
  import context.dispatcher
  import production.driver.api._

  // fields
  @transient
  private[this] var accountsUpdateTime = 0L
  @transient
  private[this] var accounts = Map.empty[AccountInfo, ActorRef]
  @transient
  private[this] var runningJobs = Map.empty[Job, Long]

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    updateAccountActor(Await.result(getAccountIds(), timeout.duration))
  }

  // functions
  private[this] def updateAccountActor(accountInfos: Seq[AccountInfo]) = {
    def initAccount(acct: AccountInfo) = {
      log.debug(s"initialize new account {}", acct.id)

      val format = new HanyuPinyinOutputFormat()
      format.setCaseType(HanyuPinyinCaseType.LOWERCASE)
      format.setToneType(HanyuPinyinToneType.WITHOUT_TONE)
      format.setVCharType(HanyuPinyinVCharType.WITH_V)

      val actorName = acct.name.map {
        case c if ActorPath.isValidPathElement(c.toString) ⇒
          c
        case c ⇒
          PinyinHelper.toHanyuPinyinStringArray(c, format)
            .headOption
            .map(_.capitalize)
            .getOrElse("")
      }.mkString

      val ref = context.actorOf(Props(classOf[FbAccountActor], acct.id, timeout, endpoint, maxRequestRetryCount).withDispatcher("dispatcher.account"), actorName)
      context.watch(ref)
      accounts += acct → ref
    }

    def removeAccount(acct: AccountInfo) = {
      log.debug(s"remove non-existed account {}", acct.id)
      val ref = accounts(acct)
      context.unwatch(ref)
      context.stop(ref)
      accounts -= acct
    }

    val availableAccounts = accountInfos.toSet
    val knownAccounts = accounts.keySet

    val newAccounts = availableAccounts.diff(knownAccounts)
    newAccounts.foreach(initAccount)
    val nonExistedAccounts = knownAccounts.diff(availableAccounts)
    nonExistedAccounts.foreach(removeAccount)

    accountsUpdateTime = System.currentTimeMillis()
    log.info(s"receive ${newAccounts.size} new and remove ${nonExistedAccounts.size} non-existed, total accounts is ${accounts.size}")
  }

  private[this] def runJobs(job: Job, s: ActorRef)(block: ⇒ Future[_]) = {
    if (runningJobs.isDefinedAt(job)) {
      val elapsed = System.nanoTime() - runningJobs(job)
      log.warning(s"job {} is current running {} sec, ignore request from {}", job.name, elapsed.nano.toSeconds, s)
    } else {
      runningJobs += job → System.nanoTime()
      log.debug("run job {} by {}", job.name, s)
      val f = block.recover { case _ ⇒ () }
      f.map(_ ⇒ JobDone(job)).pipeTo(self)
    }
  }

  type TaskResult = Seq[JsonNode]

  def getJsonNodes(job: Job, s: ActorRef): Future[Map[AccountInfo, Seq[JsonNode]]] = {

    val map: Map[AccountInfo, Future[TaskResult]] = accounts.mapValues {
      ref ⇒
        val f = ref.ask(job)(360 seconds)
        f.recover {
          case t: FbConnectionException ⇒
            log.error(t, "getJsonNodes error")
            s ! SetupSystem
            Seq.empty[JsonNode]
          case _ ⇒
            Seq.empty[JsonNode]
        }
    }.mapValues(_.mapTo[TaskResult])

    import com.pubgame.itd.util.Helper.FutureValueMapHelper
    map.toFutureSequenceMap

  }

  def updateJsonNodes(name: String, data: Future[Map[AccountInfo, TaskResult]], func: TaskResult ⇒ Future[_]) = {
    data.map {
      _.map {
        case (acct, value) ⇒
          val f1 = func(value)
          f1.recover {
            case NonFatal(t) ⇒
              log.error(t, "write {} error in {}", name, acct.name)
              ()
          }
          f1
      }
    }
  }

  def writeAccount(accounts: JsonNode*) = production.db.run {
    val businessAccounts: Seq[BusinessAccounts] = Await.result(getBusinessAccountIds, timeout.duration)

    def fromJson(json: JsonNode): AccountJsonRow = {
      val id = json.get("id").textValue()
      val name = json.get("name").textValue()
      val business: Option[BusinessAccounts] = businessAccounts.find(_.accounts.contains(id))
      AccountJsonRow(
        id,
        name,
        business.map(_.business),
        business.map(_.corp),
        business.map(_.dept),
        json
      )
    }

    DBIO.sequence(accounts.map(json ⇒ Tables.account.insertOrUpdate(fromJson(json)))).transactionally
  }

  def writeCampaign(campaigns: JsonNode*) = production.db.run {
    DBIO.sequence(campaigns.map(json ⇒ Tables.campaign.insertOrUpdate(JsonRow(json.get("id").textValue(), json)))).transactionally
  }

  val packageRegx = """(http://play.google.com/store/apps/details\?id=|https://itunes.apple.com/app/)([a-zA-Z_0-9.]*)""".r

  def writeAdSet(adSets: JsonNode*) = {

    val rows = adSets.map {
      json ⇒
        val promotedObject = json.path("promoted_object")
        val applicationId: Option[String] = Option(promotedObject.get("application_id")).map(_.asText())
        val packageName: Option[String] = Option(promotedObject.get("object_store_url")).map {
          _.asText() match {
            case packageRegx(_, p) ⇒ p
          }
        }
        AdSetRow(json.get("id").textValue(), getSpecId(json), json, applicationId, packageName)
    }
    production.db.run {
      DBIO.sequence(rows.map(row ⇒ Tables.adSet.insertOrUpdate(row))).transactionally
    } //.map(_=>log.info("write adset end: {}",adSets.take(1).toString().take(50)))
  }

  def writeAd(ads: JsonNode*) = production.db.run {
    def fromJson(json: JsonNode): AdJsonRow = {
      val id = json.get("id").textValue()
      val adName = parseAdName(json.get("name").textValue())

      AdJsonRow(
        id,
        adName.map(_.code),
        adName.map(_.region),
        adName.map(_.creative),
        adName.map(_.gender),
        adName.map(_.ageMin),
        adName.map(_.ageMax),
        adName.map(_.audience),
        Option(json.path("creative").get("id")).map(_.textValue()),
        json
      )
    }

    DBIO.sequence(ads.map(json ⇒ Tables.ad.insertOrUpdate(fromJson(json)))).transactionally
  }

  val ChannelCodeRegx = """[a-zA-Z].*&g=([a-zA-Z].*)&ch=([a-zA-Z].*)""".r

  def writeCreative(creatives: JsonNode*) = production.db.run {
    val rows = creatives.map {
      json ⇒
        val story = json.path("object_story_spec")
        val channelCode = Option(story.path("link_data").get("link"))
          .orElse(Option(story.path("video_data").path("call_to_action").path("value").get("link")))
          .map {
            _.asText() match {
              case ChannelCodeRegx(code, channel) ⇒
                (code, channel)
            }
          }
        CreativeRow(json.get("id").textValue(), json, channelCode.map(_._1), channelCode.map(_._2))
    }
    DBIO.sequence(rows.map(Tables.creative.insertOrUpdate(_)))
    //.transactionally.withTransactionIsolation(TransactionIsolation.ReadUncommitted)
  }

  def writeAudience(audiences: JsonNode*) = production.db.run {
    DBIO.sequence(audiences.map(json ⇒ Tables.audience.insertOrUpdate(JsonRow(json.get("id").textValue(), json))))
    //.transactionally.withTransactionIsolation(TransactionIsolation.ReadUncommitted)
  }

  def refreshMonitorTables: Future[_] = {

    refreshMview("monitor_mview_v2.valid_adid").flatMap {
      _ ⇒
        Future.sequence {
          Seq(
            refreshMview("monitor_mview_v2.mobile_sp_order"),
            refreshMview("monitor_mview_v2.web_sp_order"),
            refreshMview("monitor_mview_v2.mobile_player_behavior"),

            refreshMview("monitor_mview_v2.web_player_behavior_new")
              .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_player_behavior") }
              .recoverWith {
                case t ⇒
                  log.error(t, "refresh monitor_mview_v2.web_player_behavior error")
                  refreshMview("monitor_mview_v2.web_player_behavior_old")
                    .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_player_behavior_new") }
                    .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_player_behavior") }
              },

            refreshMview("monitor_mview_v2.mobile_app_device"),
            refreshMview("monitor_mview_v2.mobile_status"),
            refreshMview("monitor_mview_v2.web_status"),
            refreshMview("monitor_mview_v2.mobile_fb"),

            refreshMview("monitor_mview_v2.web_fb_new")
              .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_fb") }
              .recoverWith {
                case t ⇒
                  log.error(t, "refresh monitor_mview_v2.web_player_behavior error")
                  refreshMview("monitor_mview_v2.web_fb_old")
                    .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_fb_new") }
                    .flatMap { _ ⇒ refreshMview("monitor_mview_v2.web_fb") }
              }
          )
        }
    }
  }

  def refreshAutoAdTables: Future[_] = production.db.run {
    DBIO.seq(
      sqlu"""refresh materialized view efunfun_mobile_mview.facebook_config_info""",
      sqlu"""refresh materialized view efunfun_mobile_mview.game_room""",
      sqlu"""refresh materialized view facebook_ad_system.application_id""",
      sqlu"""refresh materialized view facebook_ad_system.fb_all""",
      sqlu"""refresh materialized view facebook_ad_system.targeting_performance"""
    )
  }

  def refreshOldTables: Future[_] = {
    Future.sequence {
      Seq(
        refreshMview("monitor_mview_v2.web_player_behavior_old"),
        refreshMview("monitor_mview_v2.web_fb_old")
      )
    }
  }

  private[this] def refreshMview(name: String): Future[_] = {
    val f = production.db.run {
      sqlu"""refresh materialized view #$name"""
    }
    f.onFailure {
      case NonFatal(t) ⇒
        log.error(t, "refresh mview {} failure", name)
    }
    f
  }

  override def receive: Receive = {
    case UpdateAccountAcotr ⇒
    // updateAccountActor(Await.result(getAccountIds(), timeout.duration))

    case ShutdownCheck ⇒

      log.info("fbRequestActor received ShutdownCheck")

      //log.info(s"accounts: ${accounts.toString}")

      accounts.values.foreach {
        ref ⇒
          println(ref)
          ref ! ShutdownCheck
      }

    case jobCreate @ CreateInsightJobInAccount(from, to) ⇒
      val s = sender()
      val jobFutures = for {
        job ← jobCreate.splitByDay(15)
        (_, account) ← accounts
      } yield {
        (account ? job).mapTo[InsightAsyncJob]
      }
      log.info("creating {} async insights jobs form {} to {} among {} accounts.", jobFutures.size, from, to, accounts.size)
      Future
        .sequence(jobFutures)
        .map(CreateInsightJobInAccountDone)
        .pipeTo(s)

    case job: Job ⇒
      val s = sender()

      try {
        runJobs(job, s) {
          log.info(s"start ${job.name}")
          job match {
            case GetAccountInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("account", result, writeAccount)
            case GetCampaignInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("info_campaign", result, writeCampaign)
            case GetAdSetInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("info_adset", result, writeAdSet)
            case GetAdInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("info_ad", result, writeAd)
            case GetAudienceInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("audience", result, writeAudience)
            case GetCreativeInfo ⇒
              val result = getJsonNodes(job, s)
              updateJsonNodes("creative", result, writeCreative)
            case RefreshMonitorTables ⇒
              refreshMonitorTables
            case RefreshAutoAdTables ⇒
              refreshAutoAdTables
            case RefreshOldTables ⇒
              refreshOldTables
            case any ⇒
              log.warning("receive unknown message {} from {}", any, s)
              Future.successful(())
          }
        }

      } catch {
        case e: Exception ⇒
          log.error(e, s"fetch fb job $job error")
          s ! SetupSystem
      }

    case JobDone(job) ⇒
      val elapsed = System.nanoTime() - runningJobs(job)
      runningJobs -= job
      log.info("{} done, time elapsed {} sec", job.name, elapsed.nanos.toSeconds)

    case Terminated(ref) ⇒
      // TODO should terminated ref unwatch?
      context.unwatch(ref)

      accounts.keySet
        .find(_.name == ref.path.name)
        .foreach(accounts -= _)

      //accounts -= accounts.filterKeys(_.name == ref.path.name).keySet.head
      //accounts -= ref.path.name

      log.warning("{} terminated, {} account left", ref.path.name, context.children.size)

    case IsAlive ⇒
      sender() ! "I am alive."

    case any ⇒
      log.warning("receive unknown message {} from {}", any, sender)
  }
}

object FbRequestActor {

  private[actor] case class AccountIds(ids: Seq[String])

  private[actor] case class BusinessAccounts(business: String, corp: String, dept: String, accounts: Seq[String] = Seq.empty)

  trait Job {
    lazy val name: String = getClass.getSimpleName.stripSuffix("$")

    override def toString: String = getClass.getSimpleName.stripSuffix("$")

  }

  case object UpdateAccountAcotr extends Job

  case object GetAccountInfo extends Job

  case object GetCampaignInfo extends Job

  case object GetAdSetInfo extends Job

  case object GetAdInfo extends Job

  case object GetAudienceInfo extends Job

  case object GetCreativeInfo extends Job

  case object RefreshMonitorTables extends Job

  case object RefreshAutoAdTables extends Job

  case object RefreshOldTables extends Job

  case class CreateInsightJobInAccount(from: LocalDate, to: LocalDate) {
    assert(!to.isBefore(from))

    def splitByDay(n: Int): Seq[CreateInsightJobInAccount] = {
      assert(n >= 1)

      @tailrec
      def splitBy0(start: LocalDate, _n: Int, result: Seq[CreateInsightJobInAccount] = Seq.empty): Seq[CreateInsightJobInAccount] = {
        val `start+n` = start.plusDays(_n)
        // start + n < to
        if (`start+n`.isBefore(to)) {
          splitBy0(`start+n`.plusDays(1), _n, result :+ CreateInsightJobInAccount(start, `start+n`))
        } else if (start.isAfter(to)) {
          //start > to
          result
        } else {
          //start <= to
          result :+ CreateInsightJobInAccount(start, to)
        }
      }

      splitBy0(from, n - 1)
    }
  }

  case class CreateInsightJobInAccountDone(jobs: Seq[InsightAsyncJob])

  private case class JobDone(job: Job)

}