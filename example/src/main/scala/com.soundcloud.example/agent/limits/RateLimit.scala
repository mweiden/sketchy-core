package com.soundcloud.example.agent.limits

import com.soundcloud.sketchy.agent.limits._
import com.soundcloud.sketchy.events.EdgeChange

/**
 * Burst Limits
 */
class ExampleBurstLimits extends BurstLimits(ExampleBurstLimits.defaults)

/**
 * Defaults
 */
object ExampleBurstLimits {
  val Minute = 60
  val Hour = 60 * Minute
  val Day = 24 * Hour

  val defaults = List(
    BurstLimit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Link, EdgeChange.Unlink),
      timeInterval = Day,
      max = 1337),
    BurstLimit(
      actionKind = "Affiliation",
      features = List(EdgeChange.Relink),
      timeInterval = Day,
      max = 13),
   BurstLimit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Link),
      timeInterval = Day,
      max = 1337),
   BurstLimit(
      actionKind = "Favoriting",
      features = List(EdgeChange.Relink),
      timeInterval = Day,
      max = 13)
      )
}
