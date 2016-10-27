package com.pubgame.itd.util

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future, Promise }

object Helper {

  implicit class FutureValueMapHelper[A, B](in: Map[B, Future[A]]) {
    def toFutureSequenceMap(implicit executor: ExecutionContext): Future[Map[B, A]] = {
      val mb = new mutable.MapBuilder[B, A, Map[B, A]](Map())
      in.foldLeft(Promise.successful(mb).future) {
        (fr, fa) ⇒
          for (r ← fr; a ← fa._2.asInstanceOf[Future[A]]) yield {
            r += ((fa._1, a))
          }
      } map (_.result)
    }
  }

}
