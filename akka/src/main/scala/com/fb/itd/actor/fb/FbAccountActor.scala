package com.pubgame.itd.actor.fb

import java.io.{ File, FileOutputStream }
import java.time.{ LocalDate, LocalDateTime }

import akka.actor.{ Actor, ActorLogging }
import akka.pattern._
import akka.util.Timeout
import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.actor.GameInfoActor.ShutdownCheck
import com.pubgame.itd.actor.fb.FbInsightActor.InsightAsyncJob
import com.pubgame.itd.actor.fb.FbRequestActor._
import com.pubgame.itd.http.FbHttpClientEndpoint

import scala.collection.immutable.LongMap
import scala.concurrent.Future
import scala.util.{ Failure, Success }

class FbAccountActor(
    id: String,
    override protected[this] val timeout: Timeout,
    override protected[this] val endpoint: FbHttpClientEndpoint,
    override protected[this] val maxRequestRetryCount: Int
) extends Actor with ActorLogging with FbRequestHelper {

  import context.dispatcher

  @transient
  var cache: Map[Long, Int] = LongMap.empty[Int]

  override def receive: Receive = {
    case CreateInsightJobInAccount(from, to) ⇒
      val s = sender()
      runInsightAsyncJobInAccount(id, from, to)
        .map(InsightAsyncJob(id, _, from, to, System.nanoTime()))
        .pipeTo(s)

    case GetAccountInfo ⇒
      val s = sender()
      getAccountInfo(id).map(Seq(_)).pipeTo(s)

    case ShutdownCheck ⇒

      log.info(s"start ShutdownCheck at $id")
      val now = LocalDate.now()

      getShutdownInfo(id, now.minusDays(1), now).onComplete {
        case Success(data) ⇒

          log.info("get " + data.size.toString + " rows")
          log.info(data.map(Mapper.treeToValue(_, classOf[Shutdown])).take(1).toString())

          //          data.map(Mapper.treeToValue(_, classOf[Shutdown])).foreach{
          //            a ⇒ log.info(a.toString.take(50))
          //          }

          context.system.terminate()

        case Failure(t) ⇒
          log.error(t, "getShutdownInfo error")
          context.system.terminate()
      }

    case job: Job ⇒

      val s = sender()

      val f = job match {
        case GetCampaignInfo ⇒
          getCampaignInfoInAccount(id)
        case GetAdSetInfo ⇒
          getAdSetInfoInAccount(id)
        case GetAdInfo ⇒
          getAdInfoInAccount(id)
        case GetCreativeInfo ⇒
          getCreativeInfoInAccount(id)
        case GetAudienceInfo ⇒
          getAudienceInfoInAccount(id)
      }

      f.onComplete {
        case Success(d) ⇒
          Future.successful(d).map(Check(job, _)).pipeTo(self)(s)
        case Failure(e) ⇒
          // log.info(s"--------------------------\n${e.toString}--------------------------\n")
          // val fos = new FileOutputStream("play/error_" + e.getClass.getSimpleName + "_" + System.currentTimeMillis())
          // val oos = new ObjectOutputStream(fos)
          // oos.writeObject(e)
          // oos.close()
          // fos.close()

          val folder = new File("play/exception")
          if (!folder.exists()) folder.mkdir()
          val fos = new FileOutputStream(s"play/exception/${System.currentTimeMillis}_${job}_${e.getClass.getSimpleName}")
          val time = LocalDateTime.now()
          val message = s"[$time] ${e.toString} \n"
          fos.write(message.getBytes)
          fos.close()

          Future.failed(e).pipeTo(s)
      }

    case Check(job, jsons) ⇒
      val time = System.nanoTime()
      val cacheSize = cache.size
      val jsonWithId = jsons.map(json ⇒ json.path("id").textValue().toLong → json)
      val newData = jsonWithId.filter {
        case (jsonId, json) ⇒
          val hash = json.##
          cache.get(jsonId) match {
            case Some(`hash`) ⇒
              false
            case _ ⇒
              cache += (jsonId → hash)
              true
          }
      }
      if (log.isInfoEnabled && newData.nonEmpty) {
        import scala.concurrent.duration._
        val elapsed = (System.nanoTime() - time).nanos.toMillis
        val insert = cache.size - cacheSize
        val update = newData.size - insert
        val jobName = job.name
        log.info(s"$jobName get ${jsons.size} rows, insert: $insert rows, update $update rows, time: $elapsed ms")
      }
      val keys = jsonWithId.map(_._1).toSet
      cache = cache.filterKeys(keys.canEqual)

      sender() ! newData.map(_._2)

    case any ⇒
      log.warning("receive unknown message {}", any)

  }

  case class Check(job: Job, jsons: Seq[JsonNode])

}