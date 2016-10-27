package com.pubgame.itd.db.json.adset

import akka.actor.Actor
import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.actor.migrate.DB
import com.pubgame.itd.db.Tables

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

trait TargetingSpecHelper {
  this: Actor ⇒
  val production: DB
  import context.dispatcher
  import production.driver.api._

  private[this]type Spec = Set[FlexibleSpec]
  //adset
  private[this] var specCache: Map[Spec, Long] = Await.result(initSpecCache, Duration.Inf)

  private[this] def initSpecCache: Future[Map[Spec, Long]] = production.db.run {
    Tables.targetingSpec.result.map {
      _.map {
        case TargetingSpecRow(json, Some(id)) ⇒
          Mapper.treeToValue(json, classOf[Array[FlexibleSpec]]).toSet → id
      }.toMap
    }
  }

  protected[this] def getSpecId(json: JsonNode): Option[Long] = {
    val flexibleSpecJson = json.path("targeting").path("flexible_spec")
    val flexibleSpec = if (flexibleSpecJson.isMissingNode) {
      Set.empty[FlexibleSpec]
    } else {
      Mapper.treeToValue(flexibleSpecJson, classOf[Array[FlexibleSpec]]).toSet
    }
    val simpleSpecJson = json.path("targeting")
    val simpleSpec = if (simpleSpecJson.isMissingNode) {
      Set.empty[FlexibleSpec]
    } else {
      Mapper.treeToValue(simpleSpecJson, classOf[SimpleSpec]).toFlexibleSpecs
    }
    val spec = flexibleSpec ++ simpleSpec
    if (spec.nonEmpty) {
      specCache.synchronized {
        specCache.get(spec) match {
          case id @ Some(_) ⇒
            id
          case _ ⇒
            val _id = production.db.run {
              (Tables.targetingSpec returning Tables.targetingSpec.map(_.id)) += TargetingSpecRow(Mapper.valueToTree(spec))
            }
            val id = Await.result(_id, Duration.Inf)
            specCache += (spec → id)
            Some(id)
        }
      }
    } else {
      None
    }
  }
}
