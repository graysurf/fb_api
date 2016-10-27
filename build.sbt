import com.pubgame.itd.{Dependencies,Settings}

lazy val root = (project in file("."))
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(
    name := "itd-root"
  ).aggregate(`db-config`, play,  akka)

lazy val `db-config` = (project in file("db-config"))
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(
    name := "itd-db-config",
    libraryDependencies ++= Dependencies.dbProject
  )

lazy val play = (project in file("play"))
  .enablePlugins(PlayScala, SbtWeb)
  .disablePlugins(PlayLayoutPlugin)
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(Settings.play: _*)
  .settings(
    name := "play",
    libraryDependencies ++= Dependencies.playProject
  ).dependsOn(akka)

lazy val akka = (project in file("akka"))
  .enablePlugins(SbtTwirl)
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(
    name := "akka",
    fork in run := true,
    libraryDependencies ++= Dependencies.akkaProject,
    libraryDependencies <++= Dependencies.util
  ).dependsOn( `db-config`)

lazy val `schema-gen` = (project in file("schema-gen"))
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(
    name := "schema-gen",
    libraryDependencies ++= Dependencies.schemaGenProject
  )


lazy val `test-project` = (project in file("test-project"))
  .settings(Settings.default: _*)
  .settings(Settings.plugin: _*)
  .settings(
    name := "test-project",
    libraryDependencies ++= Dependencies.schemaGenProject
  )

