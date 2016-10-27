package com.pubgame.alphakigo.util.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter
import com.pubgame.alphakigo.util.ansi.AnsiColor

class HighlightingConverter extends CompositeConverter[ILoggingEvent] with AnsiColor {
  override def transform(event: ILoggingEvent, in: String): String = {
    val sb = StringBuilder.newBuilder
    sb ++= ESC_START
    event.getLevel match {
      case Level.TRACE ⇒
        sb ++= BLUE
      case Level.DEBUG ⇒
        sb ++= CYAN
      case Level.INFO ⇒
        sb ++= GREEN
      case Level.WARN ⇒
        sb ++= BOLD
        sb ++= YELLOW
      case Level.ERROR ⇒
        sb ++= BOLD
        sb ++= RED
    }
    sb ++= ESC_END
    sb ++= in
    sb ++= ESC_START
    sb ++= RESET
    sb ++= ESC_END
    sb.mkString
  }
}

