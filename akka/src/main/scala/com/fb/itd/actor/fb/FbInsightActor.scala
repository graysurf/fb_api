package com.pubgame.itd.actor.fb

import java.time.LocalDate

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern._
import akka.routing._
import akka.util.Timeout
import com.pubgame.itd.actor.fb.FbInsightActor._
import com.pubgame.itd.actor.fb.FbRequestActor.{ CreateInsightJobInAccount, CreateInsightJobInAccountDone }
import com.pubgame.itd.actor.migrate.DB
import com.pubgame.itd.http.FbHttpClientEndpoint

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

class FbInsightActor(
    fbRequestActor: ActorRef,
    production: DB,
    override protected[this] implicit val timeout: Timeout,
    override protected[this] val endpoint: FbHttpClientEndpoint,
    override protected[this] val maxRequestRetryCount: Int
) extends Actor with ActorLogging with FbRequestHelper {

  // fields
  //private[this] val asyncActor = context.actorOf(Props(classOf[FbAsyncActor], production, timeout, endpoint, maxRequestRetryCount), "fb_async")
  private[this] val asyncActor = {
    val props = Props(classOf[FbAsyncActor], production, timeout, endpoint, maxRequestRetryCount)
    val pool = SmallestMailboxPool(32)
      .props(props)
      .withDispatcher("dispatcher.insight")
    context.actorOf(pool, "fb_async")
  }
  @transient
  private[this] var asyncJobs = Map.empty[(LocalDate, LocalDate), Map[String, InsightAsyncJob]]

  // functions
  override def receive: Receive = {
    case SubmitInsightJob(from, to) ⇒
      assert(!to.isBefore(from))

      try {
        val CreateInsightJobInAccountDone(jobs) = Await.result(fbRequestActor ? CreateInsightJobInAccount(from, to), Duration.Inf)
        asyncJobs.get((from, to)).foreach {
          oldJobs ⇒
            if (oldJobs.nonEmpty) {
              log.warning("There is {} not complete jobs from {} to {}:\n{}", oldJobs.size, from, to, oldJobs.values.mkString("\n"))
            }
        }
        asyncJobs += ((from, to) → jobs.map(j ⇒ j.jobId → j).toMap)
        sender() ! AskSuccess
      } catch {
        case NonFatal(t) ⇒
          sender() ! AskError(t)
      }

    case jobDone @ InsightAsyncJobsDone(doneId) ⇒
      asyncJobs = asyncJobs.map {
        case (key, map) if map.isDefinedAt(doneId) ⇒
          val asyncDone @ InsightAsyncJob(accountId, jobId, from, to, startTime, None, None) = map(doneId)
          val now = System.nanoTime()
          if (log.isDebugEnabled) {
            log.debug(s"async insight $jobId [from=$from,to=$to,account=$accountId] done, time elapsed ${(now - startTime).nanos.toSeconds} secs")
          }
          asyncActor ! GetInsight(jobId)

          key → (map + (doneId → asyncDone.copy(fetchStart = Some(now))))
        case other ⇒
          other
      }

    case InsightWriteJobsDone(doneId) ⇒
      asyncJobs = asyncJobs.map {
        case (key, map) if map.isDefinedAt(doneId) ⇒
          if (log.isDebugEnabled) {
            val InsightAsyncJob(accountId, `doneId`, from, to, startTime, Some(fetchStart), _) = map(doneId)
            val async = (fetchStart - startTime).nanos.toSeconds
            val write = (System.nanoTime() - fetchStart).nanos.toMillis
            log.debug(s"write insight $doneId [from=$from,to=$to,account=$accountId] done, time elapsed [async=$async secs,write=$write millis]")
          }
          key → (map - doneId)

        case other ⇒
          other
      }

    case InsightWriteJobsError(errorId, t) ⇒
      asyncJobs = asyncJobs.map {
        case (key, map) if map.isDefinedAt(errorId) ⇒
          val asyncError @ InsightAsyncJob(accountId, `errorId`, from, to, startTime, Some(fetchStart), _) = map(errorId)
          val async = (fetchStart - startTime).nanos.toSeconds
          val fetch = (System.nanoTime() - fetchStart).nanos.toMillis
          if (log.isErrorEnabled) {
            log.error(t, s"async insight $errorId [from=$from,to=$to,account=$accountId] error, time elapsed [async=$async secs,fetch=$fetch millis]")
          }

          key → (map + (errorId → asyncError.copy(error = Some(t))))

        case other ⇒
          other
      }

    case GetAsyncJobInfo ⇒
      sender() ! AsyncJobInfo(asyncJobs)

    case any ⇒
      log.warning("receive unknown message {} from {}", any, sender)
  }
}

object FbInsightActor {

  //  case class SubmitInsightJobDone(from: LocalDate, to: LocalDate, jobs: Seq[InsightAsyncJob])

  case class InsightAsyncJob(accountId: String, jobId: String, from: LocalDate, to: LocalDate, startTime: Long, fetchStart: Option[Long] = None, error: Option[Throwable] = None) {
    override def toString: String = {
      val async = fetchStart.map(t ⇒ s"elapsed ${(t - startTime).nanos.toSeconds} secs").getOrElse(s"not complete since ${(System.nanoTime - startTime).nanos.toSeconds} secs ago")
      val e = error.map(t ⇒ s", $t occurred").getOrElse("")
      s"$jobId in account $accountId $from - $to  async $async$e"
    }
  }

  case class InsightWriteJobsDone(id: String)

  case class InsightWriteJobsError(id: String, t: Throwable)

  case class GetInsight(id: String)

  trait FbInsightActorMessage

  // play to actor
  case class SubmitInsightJob(from: LocalDate, to: LocalDate) extends FbInsightActorMessage

  case class InsightAsyncJobsDone(id: String) extends FbInsightActorMessage

  case object GetAsyncJobInfo extends FbInsightActorMessage

  case class AsyncJobInfo(info: Map[(LocalDate, LocalDate), Map[String, InsightAsyncJob]]) extends FbInsightActorMessage

  case object AskSuccess

  case class AskError(t: Throwable)

}