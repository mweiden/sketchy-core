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

/**
 * Detection tests for JunkDetector
 */
class JunkDetectorAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {

  behavior of "The junk detector agent"

  private var ctx: MemoryContext[JunkStatistics] = _
  private var agent: JunkDetectorAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[JunkStatistics]()
    agent = new JunkDetectorAgent(ctx, minSpams = 3, confidence = 0.7)
  }

  // there are internal helpers below.
  it should "detect junk of confidence 0.7 and minSpams of 3" in {
    junkStats("junk.positive").map(ctx.append(1, _))

    agent.on(UserAction(List(1))).headOption match {
      case Some(signal: SketchySignal) =>
        assert(signal.items.toSet === Set(2, 4, 5))
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

}
