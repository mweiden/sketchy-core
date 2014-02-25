package com.soundcloud.example.agent.limits

import com.soundcloud.sketchy.agent.limits._
import com.soundcloud.sketchy.events.EdgeChange

/**
 * Limits
 */
class ExampleLimits extends Limits(ExampleLimits.defaults)

/**
 * Defaults
 */
object ExampleLimits {
  val Minute = 60
  val Hour = 60 * Minute
  val Day = 24 * Hour

  val defaults = List(
    Limit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Link, EdgeChange.Unlink),
      timeInterval = Day,
      limit = 1337),
    Limit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Relink),
      timeInterval = Day,
      limit = 13),
   Limit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Link),
      timeInterval = Day,
      limit = 1337),
   Limit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Relink),
      timeInterval = Day,
      limit = 13)
      )
}
