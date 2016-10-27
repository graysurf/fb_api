package com.pubgame.itd.db.schedule

import java.sql.Timestamp

import com.pubgame.itd.dbconfig.ModifiedPostgresDriver.api._

class ScheduleTable(_tableTag: Tag, _tableName: String) extends Table[ScheduleRow](_tableTag, Some("schedule"), _tableName) {
  val name: Rep[String] = column[String]("name", O.PrimaryKey)
  val status: Rep[String] = column[String]("status")
  val cron: Rep[String] = column[String]("cron")
  val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
  val updateTime: Rep[Option[Timestamp]] = column[Option[Timestamp]]("update_time", O.Default(None))

  def * = (name, status, cron, description, updateTime) <> (ScheduleRow.tupled, ScheduleRow.unapply)
}

class ScheduleTimeTable(_tableTag: Tag, _tableName: String) extends Table[ScheduleTimeRow](_tableTag, Some("schedule"), _tableName) {
  val name: Rep[String] = column[String]("name", O.PrimaryKey)
  val lastTime: Rep[Timestamp] = column[Timestamp]("last_time")
  val updateTime: Rep[Option[Timestamp]] = column[Option[Timestamp]]("update_time", O.Default(None))

  def * = (name, lastTime) <> (ScheduleTimeRow.tupled, ScheduleTimeRow.unapply)
}

class ScheduleTypeTable(_tableTag: Tag, _tableName: String) extends Table[ScheduleTypeRow](_tableTag, Some("schedule"), _tableName) {
  val name: Rep[String] = column[String]("name", O.PrimaryKey)
  val cron: Rep[String] = column[String]("cron")
  val range: Rep[Int] = column[Int]("range")
  val updateTime: Rep[Option[Timestamp]] = column[Option[Timestamp]]("update_time")

  def * = (name, cron, range, updateTime) <> (ScheduleTypeRow.tupled, ScheduleTypeRow.unapply)
}

