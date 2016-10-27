package com.pubgame.itd.http.model.response

import com.fasterxml.jackson.annotation.JsonProperty

case class AnsycResponse(@JsonProperty("report_run_id") report_run_id: String)