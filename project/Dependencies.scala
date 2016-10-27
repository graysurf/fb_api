package com.pubgame.itd

import play.sbt.PlayImport._
import sbt._
import sbt.Keys._

trait Dependencies {
  private[this] val AkkaVersion = "2.4.10"
  private[this] val JacksonVersion = "2.7.4"
  private[this] val LogbackVersion = "1.1.7"
  private[this] val Slf4jVersion = "1.7.21"
  private[this] val SlickVersion = "3.1.1"
  private[this] val ScalaTestVersion = "3.0.0-M15"

  lazy val Akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % AkkaVersion
  )

  lazy val Logging = Seq(
    "org.slf4j" % "slf4j-api" % Slf4jVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion
  )

  lazy val JDBC = Seq(
    "org.postgresql" % "postgresql" % "9.4.1208",
    "mysql" % "mysql-connector-java" % "5.1.39"
  )

  lazy val Play = Seq(
    cache,
    ws,
    filters,
    json
  )

  lazy val Slick = Seq(
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion exclude("com.zaxxer", "HikariCP-java6"),
    "com.zaxxer" % "HikariCP" % "2.4.6"
  )

  lazy val SlickCodeGen = Seq(
    "com.typesafe.slick" %% "slick-codegen" % SlickVersion
  )

  lazy val Jackson = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.3",
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion
  )

  lazy val TestLib = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestVersion,
    "org.mockito" % "mockito-all" % "2.0.2-beta"
  )

  lazy val util = scalaVersion.apply {
    v =>
      Seq(
//        "com.pubgame.alphakigo" %% "util" % "0.2.4",
        "org.scala-lang" % "scala-reflect" % v,
        "org.scala-lang" % "scala-compiler" % v,
        "com.belerweb" % "pinyin4j" % "2.5.1"
      )
  }

  lazy val AkkaProject = Seq(
    "org.quartz-scheduler" % "quartz" % "2.2.3"//,
//    "org.apache.httpcomponents" % "httpclient" % "4.5.2",
//    "com.google.guava" % "guava" % "19.0.20150826",
//    "org.apache.commons" % "commons-email" % "1.4"
  )

  lazy val akkaProject = Akka ++ Logging ++ Jackson ++ AkkaProject
  lazy val playProject = Play
  lazy val dbProject = Slick ++ JDBC ++ Logging.map(_ % Provided)
  lazy val schemaGenProject = SlickCodeGen ++ JDBC ++ Logging
}

object Dependencies extends Dependencies