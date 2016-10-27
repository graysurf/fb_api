package com.pubgame.itd.actor.fb

import java.io.{ ByteArrayOutputStream, IOException, PrintStream }
import java.time.LocalDate

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model.Uri
import akka.pattern._
import akka.stream.StreamTcpException
import akka.util.Timeout
import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.itd.actor.GameInfoActor.FbConnectionException
import com.pubgame.itd.actor.fb.FbRequestActor.BusinessAccounts
import com.pubgame.itd.actor.fb.FbRequestHelper._
import com.pubgame.itd.http.FbHttpClientEndpoint
import com.pubgame.itd.http.model.response.{ Cursors, ErrorResponse, Paging, SuccessResponse }
import com.pubgame.itd.http.model.{ ApiRequest, InsightRequest }

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

trait FbRequestHelper {
  this: Actor with ActorLogging ⇒

  import context.dispatcher

  protected[this] implicit def timeout: Timeout

  protected[this] def maxRequestRetryCount: Int

  protected[this] def endpoint: FbHttpClientEndpoint

  private[this] def doRequest(apiRequest: ApiRequest): Future[Seq[JsonNode]] = {
    def doRequest0(req: ApiRequest, result: Seq[JsonNode], retryCount: Int): Future[Seq[JsonNode]] = {
      val f = endpoint.ref ? req
      //val f = endpoint.request(req)
      f.flatMap {
        // single response
        case Success(SuccessResponse(data, paging)) if paging.isEmpty || data.isEmpty ⇒
          log.debug("[{}] done", req.endpoint)
          Future.successful(result ++ data)

        // multiple responses
        case Success(SuccessResponse(data, Some(Paging(Some(Cursors(_, afterCursor)), _, _)))) ⇒
          log.debug("[{}] has next page {}", req.endpoint, afterCursor)
          val nextRequest = req.copy(after = Some(afterCursor))
          //val nextRequest = ApiRequest(req.endpoint, req.fields, req.limit, Some(afterCursor), req.other)
          doRequest0(nextRequest, result ++ data, retryCount)

        case Success(SuccessResponse(data, Some(Paging(_, Some(next), _)))) if Uri(next).rawQueryString.exists(_.contains("offset")) ⇒
          log.debug("[{}] has next page {}", req.endpoint, next)
          val nextRequest = req.copy(other = req.other + ("offset" → req.limit.get.toString))
          //val nextRequest = ApiRequest(req.endpoint, req.fields, req.limit, req.after, req.other + ("offset" -> req.limit.get.toString))
          doRequest0(nextRequest, result ++ data, retryCount)

        // error
        case Success(e: ErrorResponse) ⇒
          val limit = 1
          if ((e.message.startsWith(FbRequestHelper.LimitMessage) || e.message.startsWith(FbRequestHelper.UnknownMessage)) && req.limit.exists(_ > limit)) {
            val amount = req.limit.get
            val reducedAmount = amount >>> 1
            log.debug("[{}] error, reduce amount from {} to {} and retry", req.endpoint, amount, reducedAmount)
            doRequest0(req.copy(limit = Some(reducedAmount)), result, retryCount)
            //doRequest0(ApiRequest(req.endpoint, req.fields, Some(reducedAmount), req.after, req.other), result, retryCount)
          } else {
            log.warning("[{}] {} error {}", req.endpoint, req, e)
            Future.failed(new Exception(s"${req.endpoint} retry fail, limit=${req.limit.getOrElse(-1)}, $e"))
          }

        // fuck
        case Failure(e: IOException) ⇒
          log.error(e, "IOException")
          Future.failed(new FbConnectionException(e))
        case Failure(e: StreamTcpException) ⇒
          log.error(e, "StreamTcpException")
          Future.failed(new FbConnectionException(e))
        case Failure(NonFatal(e)) ⇒
          if (maxRequestRetryCount < retryCount) {
            log.error(e, "[{}] error, exceed max retry counts {}", req.endpoint, maxRequestRetryCount)
            Future.failed(e)
          } else {
            log.warning("[{}] {} error, retry {} times, [Exception class: {}]", req.endpoint, Option(e.getMessage).getOrElse(e.getClass), retryCount, e.getClass)
            doRequest0(req, result, retryCount + 1)
          }
      }
    }

    doRequest0(apiRequest, Seq.empty, 0)
  }

  def throwableToString(throwable: Throwable): String = {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos, false, "utf-8")
    throwable.printStackTrace(ps)
    ps.close()
    baos.toString("utf-8")
  }

  private[this] def doInsightAsyncRequest(asyncRequest: InsightRequest): Future[JsonNode] = {
    def doInsightAsyncRequest0(req: InsightRequest, retryCount: Int): Future[JsonNode] = {
      endpoint.ref ? req flatMap {
        //endpoint.request(req).flatMap {
        // single response
        case Success(SuccessResponse(data, paging)) ⇒
          log.debug("[{}] done", req.endpoint)
          Future.successful(data.head)

        // error
        case Success(e: ErrorResponse) ⇒
          log.debug("[{}] {} error {}", req.endpoint, req, e)
          Future.failed(new Exception(s"${req.endpoint} json error, $e"))

        // fuck
        case Failure(NonFatal(e)) ⇒
          if (maxRequestRetryCount < retryCount) {
            log.error(e, "[{}] e error, exceed max retry counts {}", req.endpoint, maxRequestRetryCount)
            Future.failed(e)
          } else {
            log.warning("[{}] {} error, retry {} times", req.endpoint, Option(e.getMessage).getOrElse(e.getClass), retryCount)
            doInsightAsyncRequest0(req, retryCount + 1)
          }
      }
    }

    doInsightAsyncRequest0(asyncRequest, 0)
  }

  private[this] def getInfo(id: String, fields: Seq[String]) = {
    doRequest(ApiRequest(s"/$id", fields)).map(_.head)
  }

  case class AccountInfo(id: String, name: String)
  def getAccountIds(parent: String = "me"): Future[Seq[AccountInfo]] = {
    doRequest(ApiRequest(s"/$parent/adaccounts", Seq("id", "name"), Some(1000)))
      .map(_.map(json ⇒ AccountInfo(json.get("id").textValue(), json.get("name").textValue())))
  }

  def getAccountInfo(accountId: String): Future[JsonNode] = {
    getInfo(accountId, AccountFields)
  }

  def getBusinessAccountIds: Future[Seq[BusinessAccounts]] = {

    val businessAccounts = Seq(
      BusinessAccounts("339926549516584", "PG", "PG-TW"),
      BusinessAccounts("1752399385036513", "PG", "PG-GD"),
      BusinessAccounts("1659283517661561", "GD", "GD-TW"),
      BusinessAccounts("1694117567490199", "GD", "GD-TH"),
      BusinessAccounts("454893384686915", "GD", "GD-HK"),
      BusinessAccounts("544898092330388", "GD", "GD-HK")
    )

    Future.sequence {
      businessAccounts.map {
        b ⇒
          val businessId = b.business
          doRequest(ApiRequest(s"/$businessId/adaccounts", Seq("id"), Some(1000))).map(_.map(_.get("id").textValue()))
            .recover { case NonFatal(_) ⇒ Seq.empty }
            .flatMap(accountIds ⇒ Future.successful(b.copy(accounts = accountIds)))
      }
    }
  }

  def runInsightAsyncJobInAccount(accountId: String, from: LocalDate, to: LocalDate): Future[String] = {
    doInsightAsyncRequest(InsightRequest(s"/$accountId/insights", from, to)).map(_.get("report_run_id").textValue())
  }

  def getInsight(reportId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$reportId/insights", limit = Some(1000)))
  }

  def getShutdownInfo(accountId: String, from: LocalDate, to: LocalDate): Future[Seq[JsonNode]] = {
    doRequest(InsightRequest(s"/$accountId/insights", from, to, ShutdownFields, other = Map("breakdowns" → "hourly_stats_aggregated_by_advertiser_time_zone")))
  }

  def getCampaignInfoInAccount(accountId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$accountId/campaigns", CampaignFields, Some(1000)))
  }

  def getCampaignInfo(campaignId: String): Future[JsonNode] = {
    getInfo(campaignId, CampaignFields)
  }

  def getAdSetInfoInAccount(accountId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$accountId/adsets", AdSetFields, Some(500)))
  }

  def getAdSetInfo(adSetId: String): Future[JsonNode] = {
    getInfo(adSetId, AdSetFields)
  }

  def getAdInfoInAccount(accountId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$accountId/ads", AdFields, Some(1000)))
  }

  def getAdInfo(adId: String): Future[JsonNode] = {
    getInfo(adId, AdFields)
  }

  def getAudienceInfoInAccount(accountId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$accountId/customaudiences", AudienceFields, Some(1000)))
  }

  def getAudienceInfo(audienceId: String): Future[JsonNode] = {
    getInfo(audienceId, AudienceFields)
  }

  def getCreativeInfoInAccount(accountId: String): Future[Seq[JsonNode]] = {
    doRequest(ApiRequest(s"/$accountId/adcreatives", CreativeFields, Some(1000)))
  }

  def getCreativeInfo(campaignId: String): Future[JsonNode] = {
    getInfo(campaignId, CreativeFields)
  }

}

object FbRequestHelper {
  private val LimitMessage = "Please reduce the amount of data you're asking for, then retry your request"
  private val UnknownMessage = "An unknown error occurred"

  // "product_id" sucks
  val InsightFields = Seq(
    "account_id",
    "account_name",
    "action_values",
    "actions",
    "ad_id",
    "ad_name",
    "adset_id",
    "adset_name",
    "app_store_clicks",
    "buying_type",
    "call_to_action_clicks",
    "campaign_id",
    "campaign_name",
    "canvas_avg_view_percent",
    "canvas_avg_view_time",
    "clicks",
    "cost_per_10_sec_video_view",
    "cost_per_action_type",
    "cost_per_inline_link_click",
    "cost_per_inline_post_engagement",
    "cost_per_total_action",
    "cost_per_unique_action_type",
    "cost_per_unique_click",
    "cost_per_unique_inline_link_click",
    "cpc",
    "cpm",
    "cpp",
    "ctr",
    "date_start",
    "date_stop",
    "deeplink_clicks",
    "frequency",
    "impressions",
    "inline_link_click_ctr",
    "inline_link_clicks",
    "inline_post_engagement",
    "newsfeed_avg_position",
    "newsfeed_clicks",
    "newsfeed_impressions",
    "objective",
    "place_page_name",
    "reach",
    "relevance_score",
    "social_clicks",
    "social_impressions",
    "social_reach",
    "social_spend",
    "spend",
    "total_action_value",
    "total_actions",
    "total_unique_actions",
    "unique_actions",
    "unique_clicks",
    "unique_ctr",
    "unique_impressions",
    "unique_inline_link_click_ctr",
    "unique_inline_link_clicks",
    "unique_link_clicks_ctr",
    "unique_social_clicks",
    "unique_social_impressions",
    "video_10_sec_watched_actions",
    "video_15_sec_watched_actions",
    "video_30_sec_watched_actions",
    "video_avg_pct_watched_actions",
    "video_avg_sec_watched_actions",
    "video_complete_watched_actions",
    "video_p100_watched_actions",
    "video_p25_watched_actions",
    "video_p50_watched_actions",
    "video_p75_watched_actions",
    "video_p95_watched_actions",
    "website_clicks",
    "website_ctr"
  )

  private val ShutdownFields = Seq(
    "campaign_id",
    "adset_id,ad_id",
    "ad_name",
    "account_id",
    "account_name",
    "spend",
    "actions"
  )

  private val AccountFields = Seq(
    "id",
    "account_groups",
    "account_id",
    "account_status",
    "age",
    "agency_client_declaration",
    "amount_spent",
    "balance",
    "business",
    "business_city",
    "business_country_code",
    "business_name",
    "business_state",
    "business_street",
    "business_street2",
    "business_zip",
    "can_create_brand_lift_study",
    "capabilities",
    "created_time",
    "currency",
    "disable_reason",
    "end_advertiser",
    "end_advertiser_name",
    "failed_delivery_checks",
    "funding_source",
    "funding_source_details",
    "has_migrated_permissions",
    "io_number",
    "is_notifications_enabled",
    "is_personal",
    "is_prepay_account",
    "is_tax_id_required",
    // "last_used_time",
    "line_numbers",
    "media_agency",
    "min_campaign_group_spend_cap",
    "min_daily_budget",
    "name",
    "offsite_pixels_tos_accepted",
    "owner",
    //"owner_business",
    "partner",
    "rf_spec",
    "spend_cap",
    "tax_id",
    "tax_id_status",
    "tax_id_type",
    "timezone_id",
    "timezone_name",
    "timezone_offset_hours_utc",
    "tos_accepted",
    "user_role"
  )
  private val CampaignFields = Seq(
    "id",
    "account_id",
    "adlabels",
    "buying_type",
    "can_use_spend_cap",
    "configured_status",
    "created_time",
    "effective_status",
    "name",
    "objective",
    "recommendations",
    "spend_cap",
    "start_time",
    "status",
    "stop_time",
    "updated_time"
  )
  private val AdSetFields = Seq(
    "account_id",
    "adlabels",
    "adset_schedule",
    "bid_amount",
    "bid_info",
    "billing_event",
    "budget_remaining",
    "campaign_id",
    "configured_status",
    "created_time",
    "creative_sequence",
    "daily_budget",
    "effective_status",
    "end_time",
    "frequency_cap_reset_period",
    "frequency_control_specs",
    "id",
    "frequency_cap",
    "is_autobid",
    "lifetime_budget",
    "lifetime_frequency_cap",
    "lifetime_imps",
    "name",
    "optimization_goal",
    "pacing_type",
    "promoted_object",
    "recommendations",
    "rf_prediction_id",
    "rtb_flag",
    "start_time",
    "status",
    "targeting",
    "updated_time",
    "use_new_app_click"
  )
  private val AdFields = Seq(
    "id",
    "account_id",
    "ad_review_feedback",
    "adlabels",
    "adset_id",
    "bid_amount",
    "bid_info",
    "bid_type",
    "campaign_id",
    "configured_status",
    "conversion_specs",
    "created_time",
    "creative",
    "effective_status",
    "last_updated_by_app_id",
    "name",
    "recommendations",
    "status",
    "tracking_specs",
    "updated_time"
  )
  private val AudienceFields = Seq(
    "id",
    "account_id",
    "approximate_count",
    "data_source",
    "delivery_status",
    "description",
    "excluded_custom_audiences",
    "external_event_source",
    "included_custom_audiences",
    //"last_used_time",
    "lookalike_audience_ids",
    "lookalike_spec",
    "name",
    "operation_status",
    "opt_out_link",
    //"owner_business",
    "permission_for_actions",
    "pixel_id",
    "retention_days",
    "rule",
    "subtype",
    "time_content_updated",
    "time_created",
    "time_updated"
  )
  private val CreativeFields = Seq(
    "id",
    "actor_id",
    "actor_image_hash",
    "actor_image_url",
    "actor_name",
    "adlabels",
    "applink_treatment",
    "body",
    //    "call_to_action_type",
    "image_crops",
    "image_hash",
    "image_url",
    "instagram_actor_id",
    "instagram_permalink_url",
    "instagram_story_id",
    "link_og_id",
    "link_url",
    "name",
    "object_id",
    "object_story_id",
    "object_story_spec",
    "object_type",
    "object_url",
    "platform_customizations",
    "product_set_id",
    "run_status",
    "template_url",
    "thumbnail_url",
    "title",
    "url_tags"
  )
}