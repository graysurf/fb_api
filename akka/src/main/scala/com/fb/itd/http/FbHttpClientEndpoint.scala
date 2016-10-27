//package com.pubgame.itd.http
//
//import java.io.InputStream
//import java.util.Collections
//import java.util.concurrent.{ ConcurrentHashMap, Semaphore }
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model.HttpEntity.Strict
//import akka.http.scaladsl.model.Uri.Path
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.settings.ConnectionPoolSettings
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.StreamConverters
//import com.fasterxml.jackson.core.JsonParseException
//import com.pubgame.alphakigo.util.Mapper
//import com.pubgame.itd.http.model.response.{ ErrorResponse, Response, SuccessResponse }
//import com.pubgame.itd.http.model.{ ApiRequest, InsightRequest, Request }
//import org.slf4j.LoggerFactory
//
//import scala.collection.JavaConverters._
//import scala.concurrent.duration._
//import scala.concurrent.{ Await, Future }
//import scala.language.postfixOps
//import scala.util.control.NonFatal
//import scala.util.{ Failure, Success, Try }
//
//class FbHttpClientEndpoint(implicit system: ActorSystem) {
//
//  import system.dispatcher
//
//  val log = LoggerFactory.getLogger(getClass)
//  def nsDurationToString(ns: Long): String = {
//    val threshold = 10000L
//    val millisecond = 1000000L
//    val second = millisecond * 1000L
//    val minute = 60L * second
//    val hour = 60L * minute
//
//    ns match {
//      case t if t < threshold ⇒
//        "%d ns".format(t)
//      case t if t < second ⇒
//        "%.3f ms".format(t.toFloat / millisecond)
//      case t if t < minute ⇒
//        "%.2f s".format(t.toFloat / second)
//      case t if t < hour ⇒
//        "%.2f m".format(t.toFloat / minute)
//      case t ⇒
//        "%.2f h".format(t.toFloat / hour)
//    }
//  }
//
//  private[this] val connectionPoolSettings = ConnectionPoolSettings(system)
//
//  val materializer: ActorMaterializer = ActorMaterializer(namePrefix = Some("http"))
//  private[this] def http = Http()
//
//  private[this] val shares = new Semaphore(32, true)
//  private[this] case class Key(request: HttpRequest, start: Long)
//  private[this] val trace = Collections.newSetFromMap(new ConcurrentHashMap[Key, java.lang.Boolean])
//
//  system.scheduler.scheduleOnce(1 minutes) {
//    val now = System.nanoTime()
//    trace.asScala.foreach {
//      case Key(request, startTime) ⇒
//        val elapsed = now - startTime
//        if (elapsed.nanos >= 1.minutes) {
//          log.warn(s"request ${request.method} ${request.uri} not complete in ${nsDurationToString(elapsed)}")
//        }
//    }
//  }
//
//  private[this] def doRequest(request: HttpRequest): Future[HttpResponse] = {
//    val tSBegin = System.nanoTime()
//    shares.acquire()
//    try {
//      val tRBegin = System.nanoTime()
//      val r = http.singleRequest(request, settings = connectionPoolSettings)(materializer)
//      val key = Key(request, tRBegin)
//      trace.add(key)
//      r.onComplete { _ ⇒
//        shares.release()
//        val tEnd = System.nanoTime()
//        assert(trace.remove(key))
//        if (log.isDebugEnabled) {
//          log.debug {
//            val wait = nsDurationToString(tRBegin - tSBegin)
//            val req = nsDurationToString(tEnd - tRBegin)
//            s"[wait=$wait, req=$req] request[${request.method} ${request.uri}] complete."
//          }
//        }
//      }
//      r
//    } catch {
//      case t: Throwable ⇒
//        shares.release()
//        throw t
//    }
//  }
//
//  private[this] def doShutdown(): Future[Unit] = {
//    Http()
//      .shutdownAllConnectionPools()
//      .recover {
//        case NonFatal(t) ⇒
//          log.warn("akka http shutdown error", t)
//      }
//      .map {
//        _ ⇒
//          materializer.shutdown()
//      }
//      .recover {
//        case NonFatal(t) ⇒
//          log.warn("akka stream materializer shutdown error", t)
//      }
//  }
//
//  private[this] val responseReceiveTimeout = 30 seconds
//  private[this] val facebookConfig = system.settings.config.getConfig("facebook")
//  private[this] val token = facebookConfig.getString("token")
//  private[this] val version = facebookConfig.getString("version")
//  private[this] val endpoint = facebookConfig.getString("endpoint")
//  private[this] val basePath = Path / s"v$version"
//
//  private[this] def createRequest(req: Request) = {
//    req match {
//      case req: ApiRequest ⇒
//        val p = basePath + req.endpoint
//        val q = req.query.+:("access_token" → token)
//        HttpRequest(uri = Uri(s"https://$endpoint").withPath(p).withQuery(q)).withHeaders(headers.Accept(MediaRange.apply(MediaTypes.`application/json`)))
//      case _req: InsightRequest ⇒
//        val req = InsightRequest.toApiRequest(_req)
//        val p = basePath + req.endpoint
//        val q = req.query.+:("access_token" → token)
//        HttpRequest(method = HttpMethods.POST, uri = Uri(s"https://$endpoint").withPath(p).withQuery(q)).withHeaders(headers.Accept(MediaRange.apply(MediaTypes.`application/json`)))
//    }
//  }
//  private[this] def parseResponse(res: HttpResponse): Response = {
//    implicit val materializer = ActorMaterializer(namePrefix = Some("http"))
//    res match {
//      case jsonResponse if jsonResponse.entity.contentType.mediaType == MediaTypes.`application/json` ⇒
//        val tree = Mapper.readTree(jsonResponse.entity.dataBytes.runWith(StreamConverters.asInputStream(15 minutes)))
//        if (tree.has("error")) {
//          Mapper.treeToValue(tree.get("error"), classOf[ErrorResponse])
//        } else if (tree.has("data")) {
//          Mapper.treeToValue(tree, classOf[SuccessResponse])
//        } else {
//          SuccessResponse(Seq(tree), None)
//        }
//      case response ⇒
//        val body = Await.result(response.entity.toStrict(responseReceiveTimeout), Duration.Inf).data.decodeString("utf-8")
//        try {
//          val tree = Mapper.readTree(body)
//          Mapper.treeToValue(tree.get("error"), classOf[ErrorResponse])
//        } catch {
//          case e: JsonParseException ⇒
//            throw new Exception(s"not json response: ${response.entity.contentType}, body: $body")
//        }
//    }
//  }
//
//  def request(r: Request): Future[Try[Response]] = {
//    val req = createRequest(r)
//    doRequest(req)
//      .map(parseResponse)
//      .map(Success(_))
//      .recover{ case t ⇒ Failure(t) }
//  }
//
//  def shutdown() = {
//    Await.result(doShutdown(), Duration.Inf)
//  }
//}