package com.pubgame.itd.db.json.adset

import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.itd.db.jsonNodeJdbcType
import com.pubgame.itd.dbconfig.ModifiedPostgresDriver.api._

class AdSetTable(_tableTag: Tag, name: String) extends Table[AdSetRow](_tableTag, Some("facebook_ad"), name) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val targetingSpecId: Rep[Option[Long]] = column[Option[Long]]("targeting_spec_id")
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")
  lazy val applicationId: Rep[Option[String]] = column[Option[String]]("application_id")
  lazy val packageName: Rep[Option[String]] = column[Option[String]]("package")

  override def * = (id, targetingSpecId, json, applicationId, packageName) <> (AdSetRow.tupled, AdSetRow.unapply)
}

class TargetingSpecTable(_tableTag: Tag, name: String) extends Table[TargetingSpecRow](_tableTag, Some("facebook_ad"), name) {
  lazy val id: Rep[Long] = column[Long]("id", O.AutoInc)
  lazy val json: Rep[JsonNode] = column[JsonNode]("json", O.PrimaryKey)

  override def * = (json, Rep.Some(id)) <> (TargetingSpecRow.tupled, TargetingSpecRow.unapply)
}