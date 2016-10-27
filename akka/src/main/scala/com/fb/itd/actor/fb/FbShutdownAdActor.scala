package com.pubgame.itd.actor.fb

import java.time.{ LocalDate, LocalDateTime }

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.util.Timeout
import com.fasterxml.jackson.annotation.JsonProperty
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.actor.GameInfoActor.ShutdownCheck
import com.pubgame.itd.actor.migrate._
import com.pubgame.itd.http.FbHttpClientEndpoint

import scala.util.{ Failure, Success }

class FbShutdownAdActor(
    fbRequestActor: ActorRef,
    production: DB,
    override protected[this] implicit val timeout: Timeout,
    override protected[this] val endpoint: FbHttpClientEndpoint,
    override protected[this] val maxRequestRetryCount: Int
) extends Actor with ActorLogging with FbRequestHelper {

  import context.dispatcher

  override def receive: Receive = {

    case ShutdownCheck ⇒

      log.info("----------- start ShutdownCheck -----------")

      fbRequestActor ! ShutdownCheck

    //          val now = LocalDate.now()
    //
    //          //getShutdownInfo("act_521247171390446", now, now).onComplete {
    //          getShutdownInfo("act_521247171390446", now, now).onComplete {
    //            case Success(data) ⇒
    //
    //              data.map(Mapper.treeToValue(_, classOf[Shutdown])).foreach{
    //                a ⇒ log.info(a.toString)
    //              }
    //
    //              context.system.terminate()
    //
    //            case Failure(t) ⇒
    //              log.error(t, "getShutdownInfo error")
    //              context.system.terminate()
    //          }

    case any ⇒
      log.warning("receive unknown message {}", any)

  }

}

case class Shutdown(
    @JsonProperty("campaign_id") campaignId: String,
    @JsonProperty("adset_id") adsetId: String,
    @JsonProperty("ad_id") adId: String,
    @JsonProperty("ad_name") adName: String,
    @JsonProperty("account_id") accountId: String,
    @JsonProperty("account_name") accountName: String,
    @JsonProperty("spend") spend: Double,
    @JsonProperty("date_start") dateStart: String,
    @JsonProperty("date_stop") dateStop: String,
    @JsonProperty("actions") actions: Option[Seq[ActionLong]],
    @JsonProperty("hourly_stats_aggregated_by_advertiser_time_zone") hour: String
) {
  val time: LocalDateTime = LocalDateTime.parse(s"${dateStart}T${hour.substring(0, 8)}")
  val install: Option[Long] = actions.flatMap(_.find(_.actionType == "mobile_app_install").map(_.value))
  val click: Option[Long] = actions.flatMap(_.find(_.actionType == "link_click").map(_.value))

}

case class ActionLong(
  @JsonProperty("action_type") actionType: String,
  @JsonProperty("value") value: Long
)