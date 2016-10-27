package com.pubgame.play.controllers

import java.time.LocalDate
import javax.inject.{ Inject, Singleton }

import akka.actor.{ ActorSystem, ExtendedActorSystem }
import akka.pattern._
import akka.util.Timeout
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.actor.GameInfoActor.SetupSystem
import com.pubgame.itd.actor.fb.FbInsightActor._
import com.pubgame.play.ItdInfoSystem
import org.slf4j.LoggerFactory
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

@Singleton
class Application @Inject() (itdInfoSystem: ItdInfoSystem, system: ActorSystem)(implicit executionContext: ExecutionContext) extends Controller {

  private[this] val log = LoggerFactory.getLogger(classOf[Application])

  def systemStatus = Action {
    val printTree = classOf[ExtendedActorSystem].getMethod("printTree")
    printTree.setAccessible(true)
    Ok(printTree.invoke(system).toString)
  }

  def setup(password: String) = Action {
    if (password == "kigosucks") {
      itdInfoSystem.gameInfoActor ! SetupSystem
      Ok("restart system")
    } else
      Unauthorized("you sucks")
  }

  def insights(from: String, to: String, password: String) = Action.async {
    if (password == "kigosucks") {
      val job = SubmitInsightJob(LocalDate.parse(from), LocalDate.parse(to))
      (itdInfoSystem.gameInfoActor ? job)
        .map {
          case AskSuccess ⇒
            Ok(s"create insight job [from=${job.from},to=${job.to}] done")
          case AskError(t) ⇒
            BadRequest(t.toString)
        }
    } else {
      Future.successful(Unauthorized("you sucks"))
    }
  }

  implicit val timeout = Timeout(1 hours)

  def jobs(password: String) = Action.async {
    if (password == "kigosucks") {
      (itdInfoSystem.gameInfoActor ? GetAsyncJobInfo).map {
        case AsyncJobInfo(info) ⇒
          val string = info.filter(_._2.nonEmpty).map {
            case ((from, to), job) ⇒
              s"""$from - $to  ${job.values.size} remaining:
                 |${job.values.mkString("  ", "\n  ", "")}
                 |
              """.stripMargin
          }.mkString("\n")
          Ok(string)
      }
    } else {
      Future.successful(Unauthorized("you sucks"))
    }
  }

  def fbHook = Action(parse.anyContent) {
    request ⇒
      request.getQueryString("hub.challenge") match {
        case Some(challenge) ⇒ Ok(challenge)
        case None            ⇒ BadRequest
      }
  }

  def fb = Action(parse.anyContent) {
    request ⇒
      Try {
        val json = Mapper.readTree(request.body.asJson.getOrElse("").toString)

        val entries = json.get("entry")
        //        entries.asScala.foreach(println)
        val changes = entries.iterator().asScala.flatMap {
          e ⇒
            assert(e.get("id").textValue() == "1625336351080617" || e.get("id").textValue() == "1664425433838375")
            val changes = e.get("changes")
            changes.iterator().asScala
        }
        //        changes.foreach(println)
        val ids = changes.filter(_.get("value").get("verb").asText() == "complete")
          .map(_.get("value").get("report_id").asText()).toSeq

        //log.info("get report_id: {}", ids.mkString(","))

        log.debug(s"get ${ids.size} FB responses.")

        ids.foreach {
          id ⇒
            itdInfoSystem.gameInfoActor ! InsightAsyncJobsDone(id)
        }

        //        gameInfoSystem.updateDataActor ! FetchInsightAd(ids)

      } match {
        case Success(_) ⇒
          Ok
        case Failure(t) ⇒
          log.error(s"get report_id error: ${request.body.asJson.getOrElse("").toString}")
          BadRequest
      }
  }
}
