package com.soundcloud.sketchy.agent.limits

import com.soundcloud.sketchy.events.{ EdgeChange, UserEvent }
import com.soundcloud.sketchy.events.Action


/**
 * A burst limit is always valid for the event (actionKind, features), for
 * example ("Affiliation", "relinks").
 *
 * It is possible to have multiple Limits for (actionKind, features), for
 * example: an affiliation link burst limit of 100 in 1 minute (must be
 * a machine), as well as a burst limit of 300 in 24 hours (user should slow
 * down).
 *
 * @param actionKind the event kind, ie "Affiliation"
 * @param features specific feature name for the kind, ie "relinks". it is
 *        possible to compose an arbitrary number of features, for example
 *        "links", "unlinks". counters will be summed in these cases.
 * @param timeInterval the number of seconds to constrain going back from now
 * @param max the maximun number of events allowed in timeInterval going back
 *        from the current time.
 */
case class BurstLimit(
  actionKind:   String,
  features:     List[Action],
  timeInterval: Int,
  max:          Int) {

  // careful - think about the burst context(s) necessary to change this
  require(timeInterval == BurstLimits.Day, "Only day time intervals only.")

  val description = List(
    features.mkString("+"),
    "%.2fhrs".format(1.0 * timeInterval / BurstLimits.Hour)).mkString("_")
}


/**
 * Burst Limits
 */
class BurstLimits(val limits: List[BurstLimit] = BurstLimits.defaults) {
  def filter(event: UserEvent): BurstLimits = {
    new BurstLimits(limits.filter(_.actionKind == event.kind))
  }
}

/**
 * Defaults
 */
object BurstLimits {
  val Minute = 60
  val Hour = 60 * Minute
  val Day = 24 * Hour

  val defaults = List(
    BurstLimit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Link, EdgeChange.Unlink),
      timeInterval = Day,
      max = 337),
   BurstLimit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Link),
      timeInterval = Day,
      max = 1337))
}
