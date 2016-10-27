package com.pubgame.itd.http

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{ Flow, Sink, Source, StreamConverters }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import com.fasterxml.jackson.core.JsonParseException
import com.pubgame.alphakigo.util.Mapper
import com.pubgame.itd.http.model.response.{ ErrorResponse, SuccessResponse }
import com.pubgame.itd.http.model.{ ApiRequest, InsightRequest, Request }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class FbHttpClientEndpoint(implicit system: ActorSystem) {
  private[this] val settings = ActorMaterializerSettings(system).withDispatcher("dispatcher.account")
  private[this] implicit val materializer: ActorMaterializer = ActorMaterializer(settings, "http")
  private[this] val responseReceiveTimeout = 30 seconds

  private[this] val facebookConfig = system.settings.config.getConfig("facebook")
  private[this] val token = facebookConfig.getString("token")
  private[this] val version = facebookConfig.getString("version")
  private[this] val endpoint = facebookConfig.getString("endpoint")

  private[this] val source = Source.actorPublisher(Props(classOf[ApiEndpoint]))
  private[this] val sink = Sink.actorSubscriber(Props(classOf[ApiSink]))

  private[this] val basePath = Path / s"v$version"
  private[this] val request = Flow[(Request, ActorRef)].map {
    case (req: ApiRequest, replyTo) ⇒
      val p = basePath + req.endpoint
      val q = req.query.+:("access_token" → token)
      HttpRequest(uri = Uri().withPath(p).withQuery(q)).withHeaders(headers.Accept(MediaRange.apply(MediaTypes.`application/json`))) → replyTo
    case (req: InsightRequest, replyTo) ⇒
      val p = basePath + req.endpoint
      val q = req.query.+:("access_token" → token)
      HttpRequest(method = HttpMethods.POST, uri = Uri().withPath(p).withQuery(q)).withHeaders(headers.Accept(MediaRange.apply(MediaTypes.`application/json`))) → replyTo
  }

  private[this] val http = Http().cachedHostConnectionPoolHttps[ActorRef](endpoint)
  private[this] val parser = Flow[(Try[HttpResponse], ActorRef)].map {
    case (tryRequest, replyTo) ⇒
      tryRequest.map {
        case jsonResponse if jsonResponse.entity.contentType.mediaType == MediaTypes.`application/json` ⇒
          val tree = Mapper.readTree(jsonResponse.entity.dataBytes.runWith(StreamConverters.asInputStream(15 minutes)))
          if (tree.has("error")) {
            Mapper.treeToValue(tree.get("error"), classOf[ErrorResponse])
          } else if (tree.has("data")) {
            Mapper.treeToValue(tree, classOf[SuccessResponse])
          } else {
            SuccessResponse(Seq(tree), None)
          }
        case response ⇒
          val body = Await.result(response.entity.toStrict(responseReceiveTimeout), Duration.Inf).data.decodeString("utf-8")
          try {
            val tree = Mapper.readTree(body)
            Mapper.treeToValue(tree.get("error"), classOf[ErrorResponse])
          } catch {
            case e: JsonParseException ⇒
              throw new Exception(s"not json response: ${response.entity.contentType}, body: $body")
          }
      } → replyTo
  }

  private[this] val graph = source via request via http via parser to sink

  lazy val ref: ActorRef = graph.run()

  def shutdown = {
    if (!materializer.isShutdown) materializer.shutdown()
  }
}