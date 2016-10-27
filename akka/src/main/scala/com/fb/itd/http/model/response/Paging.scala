package com.pubgame.itd.http.model.response

case class Paging(
  cursors: Option[Cursors],
  next: Option[String],
  previous: Option[String]
)
