package com.pubgame.itd.db.json

import java.sql.Timestamp

import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.itd.db.jsonNodeJdbcType
import com.pubgame.itd.dbconfig.ModifiedPostgresDriver.api._

class JsonTable(_tableTag: Tag, _tableName: String) extends Table[JsonRow](_tableTag, Some("facebook_ad"), _tableName) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")

  override def * = (id, json) <> (JsonRow.tupled, JsonRow.unapply)
}

class AccountTable(_tableTag: Tag, _tableName: String) extends Table[AccountJsonRow](_tableTag, Some("facebook_ad"), _tableName) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val name: Rep[String] = column[String]("name")
  lazy val businessId: Rep[Option[String]] = column[Option[String]]("business_id")
  lazy val corp: Rep[Option[String]] = column[Option[String]]("corp")
  lazy val dept: Rep[Option[String]] = column[Option[String]]("dept")
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")

  override def * = (id, name, businessId, corp, dept, json) <> (AccountJsonRow.tupled, AccountJsonRow.unapply)
}

class InsightTable(_tableTag: Tag, _tableName: String) extends Table[InsightJsonRow](_tableTag, Some("facebook_ad"), _tableName) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val dateStart: Rep[Timestamp] = column[Timestamp]("date_start", O.PrimaryKey)
  lazy val code: Rep[Option[String]] = column[Option[String]]("code")
  lazy val region: Rep[Option[String]] = column[Option[String]]("region")
  lazy val creative: Rep[Option[String]] = column[Option[String]]("creative")
  lazy val gender: Rep[Option[String]] = column[Option[String]]("gender")
  lazy val ageMin: Rep[Option[Int]] = column[Option[Int]]("age_min")
  lazy val ageMax: Rep[Option[Int]] = column[Option[Int]]("age_max")
  lazy val audience: Rep[Option[String]] = column[Option[String]]("audience")
  lazy val action: Rep[JsonNode] = column[JsonNode]("action")
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")

  override def * = (id, dateStart, code, region, creative, gender, ageMin, ageMax, audience, action, json) <> (InsightJsonRow.tupled, InsightJsonRow.unapply)
}

class AdTable(_tableTag: Tag, _tableName: String) extends Table[AdJsonRow](_tableTag, Some("facebook_ad"), _tableName) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val code: Rep[Option[String]] = column[Option[String]]("code")
  lazy val region: Rep[Option[String]] = column[Option[String]]("region")
  lazy val creative: Rep[Option[String]] = column[Option[String]]("creative")
  lazy val gender: Rep[Option[String]] = column[Option[String]]("gender")
  lazy val ageMin: Rep[Option[Int]] = column[Option[Int]]("age_min")
  lazy val ageMax: Rep[Option[Int]] = column[Option[Int]]("age_max")
  lazy val audience: Rep[Option[String]] = column[Option[String]]("audience")
  lazy val creativeId: Rep[Option[String]] = column[Option[String]]("creative_id")
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")

  override def * = (id, code, region, creative, gender, ageMin, ageMax, audience, creativeId, json) <> (AdJsonRow.tupled, AdJsonRow.unapply)
}

class CreativeTable(_tableTag: Tag, _tableName: String) extends Table[CreativeRow](_tableTag, Some("facebook_ad"), _tableName) {
  lazy val id: Rep[String] = column[String]("id", O.PrimaryKey)
  lazy val json: Rep[JsonNode] = column[JsonNode]("json")
  lazy val code: Rep[Option[String]] = column[Option[String]]("code")
  lazy val channel: Rep[Option[String]] = column[Option[String]]("channel")

  override def * = (id, json, code, channel) <> (CreativeRow.tupled, CreativeRow.unapply)
}