package com.pubgame.itd.quartz

import org.quartz._
import org.quartz.core.jmx.JobDataMapSupport
import org.quartz.impl.StdSchedulerFactory

import scala.collection.JavaConversions._

object QuartzScheduler {

  private[this] lazy val scheduler = {
    val s = new StdSchedulerFactory().getScheduler
    sys.addShutdownHook(s.shutdown())
    s.start()
    s
  }

  def newJob(name: String, cron: String)(jobFunc: ⇒ Unit): Unit = {
    val data = JobDataMapSupport.newJobDataMap(Map[String, AnyRef]("job" → { () ⇒ jobFunc }))

    val job = JobBuilder.newJob(classOf[ExecuteJob])
      .usingJobData(data)
      .withIdentity(name, "group")
      .build()

    val trigger = TriggerBuilder
      .newTrigger()
      .withIdentity(name, "group")
      .withSchedule(CronScheduleBuilder.cronSchedule(cron))
      .build()

    scheduler.scheduleJob(job, trigger)
  }

  @DisallowConcurrentExecution
  class ExecuteJob extends Job {
    override def execute(context: JobExecutionContext): Unit = {
      context.getMergedJobDataMap.get("job").asInstanceOf[() ⇒ Unit].apply()
    }
  }
}

