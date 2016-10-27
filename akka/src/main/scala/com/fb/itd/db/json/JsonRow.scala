package com.pubgame.itd.db.json

import java.sql.Timestamp

import com.fasterxml.jackson.databind.JsonNode

case class JsonRow(id: String, data: JsonNode)

case class AccountJsonRow(
  id: String,
  name: String,
  business_id: Option[String] = None,
  corp: Option[String] = None,
  dept: Option[String] = None,
  data: JsonNode
)

case class AdJsonRow(
  id: String,
  code: Option[String] = None,
  region: Option[String] = None,
  creative: Option[String] = None,
  gender: Option[String] = None,
  ageMin: Option[Int] = None,
  ageMax: Option[Int] = None,
  audience: Option[String] = None,
  creativeId: Option[String] = None,
  data: JsonNode
)

case class InsightJsonRow(
  id: String,
  dateStart: Timestamp,
  code: Option[String] = None,
  region: Option[String] = None,
  creative: Option[String] = None,
  gender: Option[String] = None,
  ageMin: Option[Int] = None,
  ageMax: Option[Int] = None,
  audience: Option[String] = None,
  action: JsonNode,
  data: JsonNode
)

case class CreativeRow(
  id: String,
  data: JsonNode,
  code: Option[String],
  channel: Option[String]
)