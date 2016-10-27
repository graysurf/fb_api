package com.pubgame.alphakigo.util.logging

import java.util.TimeZone

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.util.CachingDateFormatter

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.Try

class AkkaDateConverter extends ClassicConverter {
  private[this] var cachingDateFormatter: CachingDateFormatter = null

  override def start(): Unit = {
    val datePattern = Option(getFirstOption) match {
      case Some(CoreConstants.ISO8601_STR) | None ⇒
        CoreConstants.ISO8601_PATTERN
      case Some(other) ⇒
        other
    }

    cachingDateFormatter = Try(new CachingDateFormatter(datePattern)) recover {
      case e: IllegalArgumentException ⇒
        addWarn("Could not instantiate SimpleDateFormat with pattern " + datePattern, e)
        new CachingDateFormatter(CoreConstants.ISO8601_PATTERN)
    } get

    val optionList = getOptionList
    if (optionList != null && optionList.size > 1) {
      val tz = TimeZone.getTimeZone(optionList.get(1))
      cachingDateFormatter.setTimeZone(tz)
    }
  }

  def convert(event: ILoggingEvent): String = {
    val mdc = event.getMDCPropertyMap.asScala
    val timestamp = mdc.get("akkaTimestamp") match {
      case Some(t) ⇒
        t.toLong
      case None ⇒
        event.getTimeStamp
    }
    cachingDateFormatter.format(timestamp)
  }

}
