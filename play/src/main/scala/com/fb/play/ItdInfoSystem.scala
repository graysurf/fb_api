package com.pubgame.play

import javax.inject.Inject

import akka.actor.{ ActorSystem, Props }
import akka.util.Timeout
import com.google.inject.Singleton
import com.pubgame.alphakigo.util.logging.Logging
import com.pubgame.itd.actor.GameInfoActor
import com.pubgame.itd.quartz.{ QuartzActor, RunQuartzMessage }
import play.api.inject.ApplicationLifecycle
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

@Singleton
class ItdInfoSystem @Inject() (applicationLifecycle: ApplicationLifecycle, system: ActorSystem)(implicit executionContext: ExecutionContext) extends Logging {

  implicit val t = Timeout(1 days)

  val pgSource = DatabaseConfig.forConfig[JdbcProfile]("db_pg_source")
  val gdSource = DatabaseConfig.forConfig[JdbcProfile]("db_gd_source")
  val target = DatabaseConfig.forConfig[JdbcProfile]("db_target")

  val gameInfoActor = system.actorOf(Props(classOf[GameInfoActor], pgSource, gdSource, target), "GameInfoActor")

  val quartzActor = system.actorOf(Props(classOf[QuartzActor], gameInfoActor, target), "quartzActor")

  quartzActor ! RunQuartzMessage

  //gameInfoActor ! GetAdInfo

  applicationLifecycle.addStopHook(() ⇒ Future.successful(system.stop(quartzActor)))
  applicationLifecycle.addStopHook(() ⇒ Future.successful(system.stop(gameInfoActor)))

}
