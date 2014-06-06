package com.soundcloud.sketchy.agent

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterEach

import com.soundcloud.sketchy.context.{
  JunkStatistics,
  Context,
  MemoryContext,
  Statistics
}

import com.soundcloud.sketchy.events.{ UserAction, SketchySignal }
import com.soundcloud.sketchy.SpecHelper

import scala.collection.immutable.HashMap

/**
 * Detection tests for JunkDetector
 */
class JunkDetectorAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {

  import JunkDetectorAgent._

  behavior of "The junk detector agent"

  private var ctx: MemoryContext[JunkStatistics] = _
  private var agent: JunkDetectorAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[JunkStatistics]()
    agent = new JunkDetectorAgent(
      statsContext = ctx,
      classes = List(
        ClassConfig(0, 0.5, 2, "Other"),
        ClassConfig(1, 0.7, 3, "Junk")))
  }

  // there are internal helpers below.
  it should "detect junk of confidence 0.7 and minSpams of 3" in {
    junkStats("junk.positive").map(ctx.append(1, _))

    agent.on(UserAction(List(1))).headOption match {
      case Some(signal: SketchySignal) => {
        assert(signal.items.toSet === Set(2, 4, 5))
        assert(signal.detector === "Junk:Junk")
      }
      case _ =>
        fail("should have detected junk")
    }
  }

  it should "NOT detect junk under confidence of 0.7" in {
    junkStats("junk.negative.below_confidence").map(ctx.append(1, _))

    agent.on(UserAction(List(1))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected junk")
      case _ =>
    }
  }

  it should "NOT detect junk under minSpams of 3" in {
    junkStats("junk.negative.below_min_spams").map(ctx.append(1, _))

    agent.on(UserAction(List(1))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected junk")
      case _ =>
    }
  }

  it should "NOT detect junk for broadcasts of different kind" in {
    junkStats("junk.negative.differing_kind").map(ctx.append(1, _))

    agent.on(UserAction(List(1))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected junk")
      case _ =>
    }
  }

  it should "have different limits for different classes" in {
    junkStats("junk.negative.breaks_limit").map(ctx.append(1, _))

    val result = agent.on(UserAction(List(1)))
    assert(result.length === 1)
    result.head match {
      case s: SketchySignal => assert(s.detector === "Junk:Other")
      case _ => fail("should have emitted a sketchy signal")
    }
  }

  it should "have different confidence levels for different classes" in {
    junkStats("junk.negative.breaks_limit_below_confidence").map(ctx.append(1, _))

    val result = agent.on(UserAction(List(1)))
    assert(result.length === 0)
  }

}
