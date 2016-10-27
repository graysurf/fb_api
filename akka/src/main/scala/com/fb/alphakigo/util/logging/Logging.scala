package com.pubgame.alphakigo.util.logging

import com.pubgame.alphakigo.util.Utils
import org.slf4j.{ LoggerFactory, Marker }

import scala.language.experimental.macros

trait Logging {
  @transient protected[logging] lazy val log = {
    // TODO: fix this workaround
    Utils.classForName("com.pubgame.alphakigo.util.logging.Logging$")
    LoggerFactory.getLogger(getClass)
  }

  // error

  protected[logging] def error(msg: String, args: Any*): Unit = macro LoggingMacro.errorMessageArgs

  protected[logging] def error(msg: String, cause: Throwable): Unit = macro LoggingMacro.errorMessageCause

  protected[logging] def error(marker: Marker, msg: String, args: Any*): Unit = macro LoggingMacro.errorMessageArgsMarker

  protected[logging] def error(marker: Marker, msg: String, cause: Throwable): Unit = macro LoggingMacro.errorMessageCauseMarker

  // warn

  protected[logging] def warn(msg: String, args: Any*): Unit = macro LoggingMacro.warnMessageArgs

  protected[logging] def warn(msg: String, cause: Throwable): Unit = macro LoggingMacro.warnMessageCause

  protected[logging] def warn(marker: Marker, msg: String, args: Any*): Unit = macro LoggingMacro.warnMessageArgsMarker

  protected[logging] def warn(marker: Marker, msg: String, cause: Throwable): Unit = macro LoggingMacro.warnMessageCauseMarker

  // info

  protected[logging] def info(msg: String, args: Any*): Unit = macro LoggingMacro.infoMessageArgs

  protected[logging] def info(msg: String, cause: Throwable): Unit = macro LoggingMacro.infoMessageCause

  protected[logging] def info(marker: Marker, msg: String, args: Any*): Unit = macro LoggingMacro.infoMessageArgsMarker

  protected[logging] def info(marker: Marker, msg: String, cause: Throwable): Unit = macro LoggingMacro.infoMessageCauseMarker

  // debug

  protected[logging] def debug(msg: String, args: Any*): Unit = macro LoggingMacro.debugMessageArgs

  protected[logging] def debug(msg: String, cause: Throwable): Unit = macro LoggingMacro.debugMessageCause

  protected[logging] def debug(marker: Marker, msg: String, args: Any*): Unit = macro LoggingMacro.debugMessageArgsMarker

  protected[logging] def debug(marker: Marker, msg: String, cause: Throwable): Unit = macro LoggingMacro.debugMessageCauseMarker

  // trace

  protected[logging] def trace(msg: String, args: Any*): Unit = macro LoggingMacro.traceMessageArgs

  protected[logging] def trace(msg: String, cause: Throwable): Unit = macro LoggingMacro.traceMessageCause

  protected[logging] def trace(marker: Marker, msg: String, args: Any*): Unit = macro LoggingMacro.traceMessageArgsMarker

  protected[logging] def trace(marker: Marker, msg: String, cause: Throwable): Unit = macro LoggingMacro.traceMessageCauseMarker
}

object Logging {
  try {
    // We use reflection here to handle the case where users remove the
    // slf4j-to-jul bridge order to route their logs to JUL.
    val bridgeClass = Utils.classForName("org.slf4j.bridge.SLF4JBridgeHandler")
    bridgeClass.getMethod("removeHandlersForRootLogger").invoke(null)
    val installed = bridgeClass.getMethod("isInstalled").invoke(null).asInstanceOf[Boolean]
    if (!installed) {
      bridgeClass.getMethod("install").invoke(null)
    }
  } catch {
    case e: ClassNotFoundException ⇒ // can't log anything yet so just fail silently
  }
}