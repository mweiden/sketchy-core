package com.soundcloud.sketchy.agent.limits

import org.scalatest.FlatSpec
import java.util.Date

import com.soundcloud.sketchy.events.{ UserEvent, EdgeChange }

import com.soundcloud.sketchy.SpecHelper


class LimitTest extends FlatSpec with SpecHelper {

  behavior of "the limit class"

  val limit = new Limit(
    "test",
    List(UserEvent.Create),
    86400,
    10.0)

  it should "check if a value breaks a maximum limit" in {
    assert(limit.doesBreak(11.0))
    assert(!limit.doesBreak(9.0))
  }

  it should "check if a value breaks a minimum limit" in {
    val minLim = limit.copy(limitType = Limit.Min)
    assert(!minLim.doesBreak(11.0))
    assert(minLim.doesBreak(9.0))
  }
}
