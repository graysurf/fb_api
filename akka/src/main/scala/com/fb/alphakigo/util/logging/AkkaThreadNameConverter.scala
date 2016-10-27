package com.pubgame.alphakigo.util.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

import scala.collection.JavaConverters._

class AkkaThreadNameConverter extends ClassicConverter {
  private[this] val AkkaDispatcherThreadName = """.+-akka\.actor\.(.+)\-([0-9]+)$""".r

  def convert(event: ILoggingEvent): String = {
    val mdc = event.getMDCPropertyMap.asScala

    mdc.getOrElse("sourceThread", event.getThreadName) match {
      case AkkaDispatcherThreadName(name, id) ⇒
        //s"${mdc("akkaSource")} @ $id"
        s"$name #$id"
      case name ⇒
        name
    }
  }
}