package com.pubgame.alphakigo.util.logging

import org.slf4j.Marker

import scala.reflect.macros.blackbox

private[logging] object LoggingMacro {

  type LoggerContext = blackbox.Context { type PrefixType = Logging }

  // Error

  def errorMessageCause(c: LoggerContext)(msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isErrorEnabled) $logger.error($msg, $cause)"
  }

  def errorMessageArgs(c: LoggerContext)(msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isErrorEnabled) $logger.error($msg)"
    } else if (args.length == 2) {
      q"if ($logger.isErrorEnabled) $logger.error($msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isErrorEnabled) $logger.error($msg, ..$args)"
    }
  }

  def errorMessageCauseMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isErrorEnabled) $logger.error($marker, $msg, $cause)"
  }

  def errorMessageArgsMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isErrorEnabled) $logger.error($marker, $msg)"
    } else if (args.length == 2) {
      q"if ($logger.isErrorEnabled) $logger.error($marker, $msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isErrorEnabled) $logger.error($marker, $msg, ..$args)"
    }
  }

  // Warn

  def warnMessageCause(c: LoggerContext)(msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isWarnEnabled) $logger.warn($msg, $cause)"
  }

  def warnMessageArgs(c: LoggerContext)(msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isWarnEnabled) $logger.warn($msg)"
    } else if (args.length == 2) {
      q"if ($logger.isWarnEnabled) $logger.warn($msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isWarnEnabled) $logger.warn($msg, ..$args)"
    }
  }

  def warnMessageCauseMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isWarnEnabled) $logger.warn($marker, $msg, $cause)"
  }

  def warnMessageArgsMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isWarnEnabled) $logger.warn($marker, $msg)"
    } else if (args.length == 2) {
      q"if ($logger.isWarnEnabled) $logger.warn($marker, $msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isWarnEnabled) $logger.warn($marker, $msg, ..$args)"
    }
  }

  // Info

  def infoMessageCause(c: LoggerContext)(msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isInfoEnabled) $logger.info($msg, $cause)"
  }

  def infoMessageArgs(c: LoggerContext)(msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isInfoEnabled) $logger.info($msg)"
    } else if (args.length == 2) {
      q"if ($logger.isInfoEnabled) $logger.info($msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isInfoEnabled) $logger.info($msg, ..$args)"
    }
  }

  def infoMessageCauseMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isInfoEnabled) $logger.info($marker, $msg, $cause)"
  }

  def infoMessageArgsMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isInfoEnabled) $logger.info($marker, $msg)"
    } else if (args.length == 2) {
      q"if ($logger.isInfoEnabled) $logger.info($marker, $msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isInfoEnabled) $logger.info($marker, $msg, ..$args)"
    }
  }

  // Debug

  def debugMessageCause(c: LoggerContext)(msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isDebugEnabled) $logger.debug($msg, $cause)"
  }

  def debugMessageArgs(c: LoggerContext)(msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isDebugEnabled) $logger.debug($msg)"
    } else if (args.length == 2) {
      q"if ($logger.isDebugEnabled) $logger.debug($msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isDebugEnabled) $logger.debug($msg, ..$args)"
    }
  }

  def debugMessageCauseMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isDebugEnabled) $logger.debug($marker, $msg, $cause)"
  }

  def debugMessageArgsMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isDebugEnabled) $logger.debug($marker, $msg)"
    } else if (args.length == 2) {
      q"if ($logger.isDebugEnabled) $logger.debug($marker, $msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isDebugEnabled) $logger.debug($marker, $msg, ..$args)"
    }
  }

  // Trace

  def traceMessageCause(c: LoggerContext)(msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isTraceEnabled) $logger.trace($msg, $cause)"
  }

  def traceMessageArgs(c: LoggerContext)(msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isTraceEnabled) $logger.trace($msg)"
    } else if (args.length == 2) {
      q"if ($logger.isTraceEnabled) $logger.trace($msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isTraceEnabled) $logger.trace($msg, ..$args)"
    }
  }

  def traceMessageCauseMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    q"if ($logger.isTraceEnabled) $logger.trace($marker, $msg, $cause)"
  }

  def traceMessageArgsMarker(c: LoggerContext)(marker: c.Expr[Marker], msg: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.log"
    if (args.isEmpty) {
      q"if ($logger.isTraceEnabled) $logger.trace($marker, $msg)"
    } else if (args.length == 2) {
      q"if ($logger.isTraceEnabled) $logger.trace($marker, $msg, Seq(${args(0)}, ${args(1)}): _*)"
    } else {
      q"if ($logger.isTraceEnabled) $logger.trace($marker, $msg, ..$args)"
    }
  }

}