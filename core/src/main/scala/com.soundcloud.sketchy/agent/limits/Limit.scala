package com.soundcloud.sketchy.agent.limits

import com.soundcloud.sketchy.events.{ EdgeChange, UserEvent }
import com.soundcloud.sketchy.events.Action


/**
 * A limit is always valid for the event (actionKind, features), for example
 * ("Affiliation", "relinks").
 *
 * It is possible to have multiple Limits for (actionKind, features), for
 * example: an affiliation link limit of 100 in 1 minute (must be a machine),
 * as well as a limit of 300 in 24 hours (user should slow down).
 *
 * @param actionKind the event kind, ie "Affiliation"
 * @param features specific feature name for the kind, ie "relinks". it is
 *        possible to compose an arbitrary number of features, for example
 *        "links", "unlinks". counters will be summed in these cases.
 * @param timeInterval the number of seconds to constrain going back from now
 * @param max the maximun number of events allowed in timeInterval going back
 *        from the current time.
 */
object Limit {
  trait Type
  case object Max extends Type
  case object Min extends Type
}

case class Limit(
  actionKind:   String,
  features:     List[Action],
  timeInterval: Int,
  limit:        Double,
  limitType:    Limit.Type = Limit.Max) {

  import Limit._

  def doesBreak(value: Double): Boolean =
    limitType match {
      case Max => value > limit
      case Min => value < limit
    }

  require(timeInterval >= Limits.Hour, "Small increments may cause prohibitive network traffic!.")

  val description = List(
    features.mkString("+"),
    "%.2fhrs".format(1.0 * timeInterval / Limits.Hour)).mkString("_")
}


/**
 * Limits
 */
class Limits(val limits: List[Limit] = Limits.defaults) {
  def filter(event: UserEvent): Limits = {
    new Limits(limits.filter(_.actionKind == event.kind))
  }
}

/**
 * Defaults
 */
object Limits {
  val Minute = 60
  val Hour = 60 * Minute
  val Day = 24 * Hour

  val defaults = List(
    Limit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Link, EdgeChange.Unlink),
      timeInterval = Day,
      limit = 337),
   Limit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Link),
      timeInterval = Day,
      limit = 1337))
}

