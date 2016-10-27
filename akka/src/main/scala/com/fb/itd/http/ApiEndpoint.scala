package com.pubgame.itd.http

import akka.actor.ActorRef
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request ⇒ StreamRequest }
import com.pubgame.itd.http.model.Request

import scala.annotation.tailrec

class ApiEndpoint extends ActorPublisher[(Request, ActorRef)] {
  private[this] final var buf: Vector[(Request, ActorRef)] = Vector.empty[(Request, ActorRef)]

  @tailrec
  private[this] final def onRequest(): Unit =
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use.foreach(onNext)
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use.foreach(onNext)
        onRequest()
      }
    }

  override final def receive: Receive = {
    case request: Request ⇒
      if (buf.isEmpty && totalDemand > 0)
        onNext(request → sender)
      else {
        buf :+= request → sender
        onRequest()
      }

    case StreamRequest(q) ⇒
      onRequest()

    case Cancel ⇒
      context.stop(self)
  }
}