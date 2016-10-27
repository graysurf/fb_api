package com.pubgame.alphakigo.util.logging

import akka.event.slf4j.Slf4jLogger

class AkkaLogger extends Slf4jLogger {
  /**
   * Override to provide millis.
   * @param timestamp a "currentTimeMillis"-obtained timestamp
   * @return the given timestamp as a currentTimeMillis String
   */
  override protected[this] def formatTimestamp(timestamp: Long): String =
    timestamp.toString
}