package com.pubgame.itd.actor

import java.sql.Timestamp
import java.text.SimpleDateFormat

package object fb {

  val FBDateFormatter: ThreadLocal[SimpleDateFormat] = ThreadLocal.withInitial(() ⇒ new SimpleDateFormat("yyyy-MM-dd"))
  def fbDate(date: String): Timestamp = {
    new Timestamp(FBDateFormatter.get().parse(date).getTime)
  }

  val AdNameRegex = "^([A-Za-z0-9,]+)_([^_]+)_([^_]+)_(M|F|0)_([0-9]+)[xX]([0-9]+)_([^_]+)(?:_(.*))?$".r
  case class AdName(
    code: String,
    region: String,
    creative: String,
    gender: String,
    ageMin: Int,
    ageMax: Int,
    audience: String
  )

  def parseAdName(name: String) = {
    name match {
      case AdNameRegex(code, region, creative, gender, ageMin, ageMax, audience, _) ⇒
        Some(AdName(code.toLowerCase, region.toLowerCase, creative.toLowerCase, gender.toLowerCase, ageMin.toInt, ageMax.toInt, audience.toLowerCase))
      case _ ⇒
        None
    }
  }

}
