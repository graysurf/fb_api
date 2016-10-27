package com.pubgame.itd.db.json.adset

import com.fasterxml.jackson.databind.JsonNode

case class AdSetRow(id: String, targetingSpecId: Option[Long], data: JsonNode, applicationId: Option[String], packageName: Option[String])

