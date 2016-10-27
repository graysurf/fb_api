package com.pubgame.itd.db.json.adset

import com.fasterxml.jackson.annotation.{ JsonIgnoreProperties, JsonInclude }
import com.fasterxml.jackson.databind.JsonNode

case class TargetingSpecRow(json: JsonNode, id: Option[Long] = None)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
case class FlexibleSpec(
  behaviors: Option[Set[IDName]] = None,
  college_years: Option[Set[Long]] = None,
  connections: Option[Set[IDName]] = None,
  custom_audiences: Option[Set[IDName]] = None,
  education_majors: Option[Set[IDName]] = None,
  education_schools: Option[Set[IDName]] = None,
  education_statuses: Option[Set[Long]] = None,
  ethnic_affinity: Option[Set[IDName]] = None,
  family_statuses: Option[Set[IDName]] = None,
  friends_of_connections: Option[Set[IDName]] = None,
  generation: Option[Set[IDName]] = None,
  home_ownership: Option[Set[IDName]] = None,
  home_type: Option[Set[IDName]] = None,
  home_value: Option[Set[IDName]] = None,
  household_composition: Option[Set[IDName]] = None,
  income: Option[Set[IDName]] = None,
  industries: Option[Set[IDName]] = None,
  interested_in: Option[Set[Long]] = None,
  interests: Option[Set[IDName]] = None,
  life_events: Option[Set[IDName]] = None,
  moms: Option[Set[IDName]] = None,
  net_worth: Option[Set[IDName]] = None,
  office_type: Option[Set[IDName]] = None,
  politics: Option[Set[IDName]] = None,
  relationship_statuses: Option[Set[Long]] = None,
  user_adclusters: Option[Set[IDName]] = None,
  work_employers: Option[Set[IDName]] = None,
  work_positions: Option[Set[IDName]] = None
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
case class SimpleSpec(
    behaviors: Option[Set[IDName]],
    college_years: Option[Set[Long]],
    connections: Option[Set[IDName]],
    custom_audiences: Option[Set[IDName]],
    education_majors: Option[Set[IDName]],
    education_schools: Option[Set[IDName]],
    education_statuses: Option[Set[Long]],
    ethnic_affinity: Option[Set[IDName]],
    family_statuses: Option[Set[IDName]],
    friends_of_connections: Option[Set[IDName]],
    generation: Option[Set[IDName]],
    home_ownership: Option[Set[IDName]],
    home_type: Option[Set[IDName]],
    home_value: Option[Set[IDName]],
    household_composition: Option[Set[IDName]],
    income: Option[Set[IDName]],
    industries: Option[Set[IDName]],
    interested_in: Option[Set[Long]],
    interests: Option[Set[IDName]],
    life_events: Option[Set[IDName]],
    moms: Option[Set[IDName]],
    net_worth: Option[Set[IDName]],
    office_type: Option[Set[IDName]],
    politics: Option[Set[IDName]],
    relationship_statuses: Option[Set[Long]],
    user_adclusters: Option[Set[IDName]],
    work_employers: Option[Set[IDName]],
    work_positions: Option[Set[IDName]]
) {

  def toFlexibleSpecs: Set[FlexibleSpec] = {
    var result = Set.empty[FlexibleSpec]
    if (behaviors.isDefined) {
      result += FlexibleSpec(behaviors = behaviors)
    }
    if (college_years.isDefined) {
      result += FlexibleSpec(college_years = college_years)
    }
    if (connections.isDefined) {
      result += FlexibleSpec(connections = connections)
    }
    if (custom_audiences.isDefined) {
      result += FlexibleSpec(custom_audiences = custom_audiences)
    }
    if (education_majors.isDefined) {
      result += FlexibleSpec(education_majors = education_majors)
    }
    if (education_schools.isDefined) {
      result += FlexibleSpec(education_schools = education_schools)
    }
    if (education_statuses.isDefined) {
      result += FlexibleSpec(education_statuses = education_statuses)
    }
    if (ethnic_affinity.isDefined) {
      result += FlexibleSpec(ethnic_affinity = ethnic_affinity)
    }
    if (family_statuses.isDefined) {
      result += FlexibleSpec(family_statuses = family_statuses)
    }
    if (friends_of_connections.isDefined) {
      result += FlexibleSpec(friends_of_connections = friends_of_connections)
    }
    if (generation.isDefined) {
      result += FlexibleSpec(generation = generation)
    }
    if (home_ownership.isDefined) {
      result += FlexibleSpec(home_ownership = home_ownership)
    }
    if (home_type.isDefined) {
      result += FlexibleSpec(home_type = home_type)
    }
    if (home_value.isDefined) {
      result += FlexibleSpec(home_value = home_value)
    }
    if (household_composition.isDefined) {
      result += FlexibleSpec(household_composition = household_composition)
    }
    if (income.isDefined) {
      result += FlexibleSpec(income = income)
    }
    if (industries.isDefined) {
      result += FlexibleSpec(industries = industries)
    }
    if (interested_in.isDefined) {
      result += FlexibleSpec(interested_in = interested_in)
    }
    if (interests.isDefined) {
      result += FlexibleSpec(interests = interests)
    }
    if (life_events.isDefined) {
      result += FlexibleSpec(life_events = life_events)
    }
    if (moms.isDefined) {
      result += FlexibleSpec(moms = moms)
    }
    if (net_worth.isDefined) {
      result += FlexibleSpec(net_worth = net_worth)
    }
    if (office_type.isDefined) {
      result += FlexibleSpec(office_type = office_type)
    }
    if (politics.isDefined) {
      result += FlexibleSpec(politics = politics)
    }
    if (relationship_statuses.isDefined) {
      result += FlexibleSpec(relationship_statuses = relationship_statuses)
    }
    if (user_adclusters.isDefined) {
      result += FlexibleSpec(user_adclusters = user_adclusters)
    }
    if (work_employers.isDefined) {
      result += FlexibleSpec(work_employers = work_employers)
    }
    if (work_positions.isDefined) {
      result += FlexibleSpec(work_positions = work_positions)
    }
    result
  }

}