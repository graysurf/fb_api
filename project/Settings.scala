package com.pubgame.itd

import com.typesafe.sbt.SbtScalariform.ScalariformKeys.preferences
import com.typesafe.sbt.SbtScalariform._
import play.sbt.PlayImport._
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import sbtassembly._
import sbtassembly.AssemblyKeys._

import scalariform.formatter.preferences._
object Settings {
  lazy val default: Seq[Setting[_]] = Seq(
    organization := "com.pubgame.itd",
    version := "0.0.1",
    crossScalaVersions := Seq("2.11.8", "2.10.6"),
    scalaVersion := crossScalaVersions.value.head,

    // compile opt
    scalacOptions <++= scalaVersion.map {
      v =>
        if (v.startsWith("2.11")) {
          newScalacOptions ++ commonScalacOptions
        } else {
          commonScalacOptions
        }
    },
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-encoding", "UTF-8",
      "-Xlint:all",
      "-Xdoclint:all",
      "-XDignore.symbol.file",
      "-g",
      "-deprecation"
    ),
    javaOptions in run ++= Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-ea"
    ),

    cancelable in Global := true,
    fork in run := false,

    // test
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
      Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")
    ),
    parallelExecution in Test := false,

    // log
    logBuffered := false,
    logLevel := Level.Info,
    persistLogLevel := Level.Info,
    ivyLoggingLevel := UpdateLogging.Default,
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },

    // repository
//    resolvers ++= Seq(
//      "Pubgame Data Team Maven" at "http://ec2-54-65-230-104.ap-northeast-1.compute.amazonaws.com/repository/maven",
//      Resolver.url("Pubgame Data Team SBT", url("http://ec2-54-65-230-104.ap-northeast-1.compute.amazonaws.com/repository/sbt"))(Resolver.ivyStylePatterns)
//    ),
    conflictManager := ConflictManager.latestRevision,
    updateOptions := updateOptions.value.withCachedResolution(true)//,
//    publishMavenStyle := false,
//    publishTo := {
//      val base = "http://ec2-54-65-230-104.ap-northeast-1.compute.amazonaws.com/repository/"
//      if (isSnapshot.value) {
//        Some(Resolver.url("Pubgame Data team Snapshot Repository", url(base + "sbt-snapshot-local"))(Resolver.defaultIvyPatterns))
//      } else {
//        Some(Resolver.url("Pubgame Data team Release Repository", url(base + "sbt-release-local"))(Resolver.defaultIvyPatterns))
//      }
//    },
//    credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
  )

  lazy val plugin: Seq[Setting[_]] =  scalariformSettings ++ Seq(
    preferences := preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignArguments, true),
    assemblyMergeStrategy in assembly := {
      case "application.conf" => MergeStrategy.concat
      case x =>
          (assemblyMergeStrategy in assembly).value(x)
    }
  )

  lazy val play: Seq[Setting[_]] = Seq(
    emojiLogs,
//    pipelineStages := Seq(filter, cssCompress, uglify, digest, gzip),
    routesGenerator := InjectedRoutesGenerator
  )

  private[this] lazy val newScalacOptions = Seq(
    "-target:jvm-1.8",
    "-Xlint:_",
    "-Ybackend:GenBCode",
    "-Ybreak-cycles",
//    "-Yconst-opt",
//    "-Ylinearizer:dump",
//    "-Yopt:_",
//    "-Yopt-inline-heuristics:everything",
    "-Ywarn-infer-any",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  )
  private[this] lazy val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-explaintypes",
    "-feature",
    "-g:vars",
//    "-optimise",
    "-unchecked",
    "-Xcheckinit",
    "-Xexperimental",
    "-Xfuture",
    "-Xlog-free-terms",
    "-Xlog-free-types",
    "-Xlog-reflective-calls",
    "-Xmigration",
    "-Xprint-types",
    "-Xxml:_",
//    "-Yclosure-elim",
    "-Ydead-code",
//    "-Ydelambdafy:inline",
    "-Yinfer-argument-types",
//    "-Yinline",
//    "-Yinline-handlers",
    "-Yrangepos",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-value-discard"
  )
}