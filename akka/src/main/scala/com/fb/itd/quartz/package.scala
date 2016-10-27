package com.pubgame.itd

package object quartz {

  case class CronData(name: String, cron: String, range: Int)
  case class ScheduleData(name: String, status: String, cron: CronData)

  case object RunQuartzMessage
}
