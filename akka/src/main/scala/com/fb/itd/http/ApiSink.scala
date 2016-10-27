package com.pubgame.itd.http

import akka.actor.ActorRef
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ ActorSubscriber, RequestStrategy, WatermarkRequestStrategy }

import scala.util.Try

class ApiSink extends ActorSubscriber {
  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(20)

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("----------------------")
    println(sender())
    println(s"mgs:$message")
    reason.printStackTrace()
    println("----------------------")
    super.preRestart(reason, message)
  }

  override def receive: Receive = {
    case OnNext((response: Try[_], ref: ActorRef)) ⇒
      ref ! response
    case any ⇒
      println(any)
  }
}