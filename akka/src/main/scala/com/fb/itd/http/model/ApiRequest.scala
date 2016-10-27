package com.pubgame.itd.http.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.Uri.Query
import com.pubgame.itd.actor.fb.FbRequestHelper

import scala.language.implicitConversions

case class ApiRequest(
    endpoint: String,
    fields: Seq[String] = Seq.empty,
    limit: Option[Int] = None,
    after: Option[String] = None,
    other: Map[String, String] = Map.empty
) extends Request {

  override def query: Query = {
    var q = Query()
    if (fields.nonEmpty) {
      q +:= "fields" → fields.mkString(",")
    }
    limit.foreach {
      l ⇒
        q +:= "limit" → l.toString
    }
    after.foreach {
      a ⇒
        q +:= "after" → a
    }
    other.foreach {
      case (key, value) ⇒
        q +:= key → value
    }
    q
  }
}

case class InsightRequest(
    endpoint: String,
    since: LocalDate,
    until: LocalDate,
    fields: Seq[String] = FbRequestHelper.InsightFields,
    level: String = "ad",
    time_increment: Int = 1,
    other: Map[String, String] = Map.empty
) extends Request {

  val param = other ++ Map(
    "level" → level,
    "time_range" → s"{'since':'${since.format(InsightRequest.Formatter)}','until':'${until.format(InsightRequest.Formatter)}'}",
    "time_increment" → time_increment.toString
  )

  override def query: Query = {
    var q = Query()

    if (fields.nonEmpty) {
      q +:= "fields" → fields.mkString(",")
    }
    param.foreach {
      case (key, value) ⇒
        q +:= key → value
    }
    q
  }

}

object InsightRequest {
  private val Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit def toApiRequest(req: InsightRequest): ApiRequest =
    ApiRequest(
      endpoint = req.endpoint,
      fields   = req.fields,
      limit    = Some(1000),
      other    = req.param
    )
}

trait Request {

  def query: Query

  def endpoint: String

}