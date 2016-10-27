package com.pubgame.alphakigo.util.ansi

/**
 * @version 0.1
 * @since 0.1
 */
private[ansi] trait AnsiBasic {
  private[this] val notWindows = {
    !Option(System.getProperty("os.name"))
      .map(_.toLowerCase)
      .exists(_.contains("windows"))
  }

  lazy val isANSISupported: Boolean = {
    Option(System.getProperty("sbt.log.noformat")).map(_ != "true")
      .getOrElse(notWindows)
  }

  final val ESC_START: String = "\u001b["
  final val ESC_END: String = "m"
}
