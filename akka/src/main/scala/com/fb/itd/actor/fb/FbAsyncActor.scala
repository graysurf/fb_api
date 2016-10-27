package com.pubgame.itd.actor.fb

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.actor.fb.FbAsyncActor.RetryGetInsight
import com.pubgame.itd.actor.fb.FbInsightActor.{ GetInsight, InsightWriteJobsDone, InsightWriteJobsError }
import com.pubgame.itd.actor.migrate.DB
import com.pubgame.itd.db.Tables
import com.pubgame.itd.db.json.InsightJsonRow
import com.pubgame.itd.http.FbHttpClientEndpoint

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.control.NonFatal

class FbAsyncActor(
    production: DB,
    override protected[this] implicit val timeout: Timeout,
    override protected[this] val endpoint: FbHttpClientEndpoint,
    override protected[this] val maxRequestRetryCount: Int
) extends Actor with ActorLogging with FbRequestHelper {

  import production.driver.api._

  def writeInsight(jobId: String, result: Seq[JsonNode]) = {
    def writeInsight0(insights: JsonNode*) = {
      def fromJson(json: JsonNode): InsightJsonRow = {
        val id = json.get("ad_id").textValue()
        val actions = json.path("actions")
        val actionJson: JsonNode = if (actions.isMissingNode) {
          Mapper.createObjectNode()
        } else {
          val map = actions.iterator().asScala.map {
            kv ⇒
              kv.get("action_type").textValue() → kv.get("value").asLong()
          }.toMap
          Mapper.valueToTree(map)
        }

        val adName = parseAdName(json.get("ad_name").textValue())
        InsightJsonRow(
          id,
          fbDate(json.get("date_start").textValue()),
          adName.map(_.code),
          adName.map(_.region),
          adName.map(_.creative),
          adName.map(_.gender),
          adName.map(_.ageMin),
          adName.map(_.ageMax),
          adName.map(_.audience),
          actionJson,
          json
        )
      }
      if (insights.nonEmpty) {
        log.debug(s"insight size: ${insights.size} -> ${insights.head.toString.take(100)}...")
        production.db.run {
          DBIO.sequence(insights.map(json ⇒ Tables.insight.insertOrUpdate(fromJson(json)))).transactionally
        }
      } else {
        Future.successful(())
      }
    }
    try {
      Await.result(writeInsight0(result: _*), Duration.Inf)
      InsightWriteJobsDone(jobId)
    } catch {
      case NonFatal(t) ⇒
        InsightWriteJobsError(jobId, t)
    }
  }

  override def receive: Receive = {
    case GetInsight(id) ⇒
      try {
        val fetchResult = Await.result(getInsight(id), Duration.Inf)
        val writeResult = writeInsight(id, fetchResult)
        sender() ! writeResult
      } catch {
        case NonFatal(t) ⇒
          log.warning("fetch {} insight error, retry 0 times", id)
          context.system.scheduler.scheduleOnce(5 seconds, self, RetryGetInsight(id))(context.dispatcher, sender())
      }

    case RetryGetInsight(id, count) ⇒
      try {
        val fetchResult = Await.result(getInsight(id), Duration.Inf)
        val writeResult = writeInsight(id, fetchResult)
        sender() ! writeResult
      } catch {
        case NonFatal(t) ⇒
          val retry = RetryGetInsight(id, count + 1)
          if (retry.count < maxRequestRetryCount) {
            log.warning("fetch {} insight error, retry {} times", id, count)
            context.system.scheduler.scheduleOnce(30 seconds, self, retry)(context.dispatcher, sender())
          } else {
            log.error(t, "fetch {} insight error, exceeded max retry count {}", id, maxRequestRetryCount)
            sender() ! InsightWriteJobsError(id, t)
          }
      }

    case any ⇒
      log.warning("receive unknown message {} from {}", any, sender)
  }
}

object FbAsyncActor {

  case class RetryGetInsight(id: String, count: Int = 0)

}