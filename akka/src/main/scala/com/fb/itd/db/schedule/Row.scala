package com.pubgame.itd.db.schedule

import java.sql.Timestamp

case class ScheduleRow(name: String, status: String, cron: String, description: Option[String] = None, updateTime: Option[java.sql.Timestamp] = None)

case class ScheduleTimeRow(name: String, lastTime: Timestamp = new Timestamp(System.currentTimeMillis))

case class ScheduleTypeRow(name: String, cron: String, range: Int, updateTime: Option[java.sql.Timestamp])