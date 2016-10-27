package com.pubgame.itd.http.model.response

import com.fasterxml.jackson.annotation.JsonProperty

case class ErrorResponse(
  message: String,
  `type`: Option[String],
  code: Long,
  @JsonProperty("error_user_title") errorUserTitle: Option[String],
  @JsonProperty("error_user_msg") errorUserMsg: Option[String],
  @JsonProperty("error_subcode") errorSubcode: Option[Long],
  @JsonProperty("fbtrace_id") fbtraceId: Option[String],
  @JsonProperty("is_transient") isTransient: Option[Boolean]
) extends Response
