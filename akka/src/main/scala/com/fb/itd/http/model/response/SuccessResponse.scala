package com.pubgame.itd.http.model.response

import com.fasterxml.jackson.databind.JsonNode

trait Response
case class SuccessResponse(
  data: Seq[JsonNode],
  paging: Option[Paging]
) extends Response
