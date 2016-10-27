package com.pubgame.itd.db

import com.pubgame.itd.db.json._
import com.pubgame.itd.db.json.adset.{ AdSetTable, TargetingSpecTable }
import com.pubgame.itd.db.schedule.{ ScheduleTable, ScheduleTimeTable, ScheduleTypeTable }
import slick.lifted.TableQuery

/*
drop schema facebook_ad CASCADE ;
create schema facebook_ad;

CREATE TABLE facebook_ad.account
(
  id text NOT NULL,
  json jsonb NOT NULL,
  business_id text,
  corp text,
  dept text,
  name text,
  CONSTRAINT account_pkey PRIMARY KEY (id)
);

CREATE TABLE facebook_ad.ad
(
  id text NOT NULL,
  json jsonb NOT NULL,
  code text,
  region text,
  creative text,
  gender text,
  age_min integer,
  age_max integer,
  audience text,
  CONSTRAINT ad_pkey PRIMARY KEY (id)
);

create table facebook_ad.targeting_spec (
  id   BIGSERIAL NOT NULL,
  json JSONB     NOT NULL,
  PRIMARY KEY (id)
);
create UNIQUE INDEX on facebook_ad.targeting_spec (json);
create table facebook_ad.ad_set (
  id               TEXT  NOT NULL,
  targeting_spec_id BIGINT REFERENCES facebook_ad.targeting_spec (id) ON UPDATE CASCADE ON DELETE SET NULL,
  json             JSONB NOT NULL,
  PRIMARY KEY (id)
);

create table facebook_ad.audience (
  id   TEXT  NOT NULL,
  json JSONB NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE facebook_ad.campaign (
  id   TEXT  NOT NULL,
  json JSONB NOT NULL,
  PRIMARY KEY (id)
);


create table facebook_ad.creative (
  id   TEXT  NOT NULL,
  json JSONB NOT NULL,
  PRIMARY KEY (id)
);

create table facebook_ad.ad_insight (
  id   TEXT  NOT NULL,
  date_start TIMESTAMP NOT NULL,
  code TEXT,
  region TEXT,
  creative TEXT,
  gender TEXT,
  age_min INT,
  age_max INT,
  audience TEXT,
  json JSONB NOT NULL,
  PRIMARY KEY (id,date_start)
);

 */
object Tables {
  val account: TableQuery[AccountTable] = TableQuery(new AccountTable(_, "account"))
  val ad: TableQuery[AdTable] = TableQuery(new AdTable(_, "ad"))
  val audience: TableQuery[JsonTable] = TableQuery(new JsonTable(_, "audience"))
  val adSet: TableQuery[AdSetTable] = TableQuery(new AdSetTable(_, "ad_set"))
  val campaign: TableQuery[JsonTable] = TableQuery(new JsonTable(_, "campaign"))
  val creative: TableQuery[CreativeTable] = TableQuery(new CreativeTable(_, "creative"))
  val targetingSpec: TableQuery[TargetingSpecTable] = TableQuery(new TargetingSpecTable(_, "targeting_spec"))
  val insight: TableQuery[InsightTable] = TableQuery(new InsightTable(_, "ad_insight"))

  val schedule: TableQuery[ScheduleTable] = TableQuery(new ScheduleTable(_, "schedule"))
  val scheduleType: TableQuery[ScheduleTypeTable] = TableQuery(new ScheduleTypeTable(_, "schedule_type"))
  val scheduleTime: TableQuery[ScheduleTimeTable] = TableQuery(new ScheduleTimeTable(_, "schedule_time"))

}