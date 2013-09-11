package com.soundcloud.sketchy.agent

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events.{ SketchySignal, UserAction }

import com.soundcloud.sketchy.SpecHelper

/**
 * Detection tests for BulkDetector
 */
class BulkDetectorAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {

  behavior of "The bulk detector"

  var ctx: MemoryContext[BulkStatistics] = _
  var agent: BulkDetectorAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[BulkStatistics]()
    agent = new BulkDetectorAgent(ctx, minSpams = 10, maxDist = 0.5)
  }

  it should "detect bulk of max Jaccard distance 0.5 and minSpams of 10" in {
    val bulk = Set(63, 70, 64, 69, 71, 65, 76, 72, 68, 77, 78, 73, 66, 67)

    bulkStats("bulk.positive").map(ctx.append(1, _))

    val response = agent.on(UserAction(List(1)))
    if (response.isEmpty) fail("should have detected bulk") else {
      val signal = response.head.asInstanceOf[SketchySignal]
      assert(signal.strength === 0.8060132942205083)
      assert(signal.items.toSet === bulk)
    }
  }

  it should "NOT detect non-bulk of max Jaccard distance 0.5 and minSpams of 10" in {
    bulkStats("bulk.multiple_users1").map(ctx.append(1, _))
    bulkStats("bulk.multiple_users2").map(ctx.append(2, _))

    val response = agent.on(UserAction(List(1))) ++ agent.on(UserAction(List(2)))
    if (!response.isEmpty) fail("should NOT have detected bulk")
  }

  it should "detect bulk of multiple users max Jaccard distance 0.5 and minSpams of 10" in {
    val bulk = Set(63, 70, 64, 69, 71, 65, 76, 72, 68, 77, 78, 73, 66, 67)

    bulkStats("bulk.multiple_users1").map(ctx.append(1, _))
    bulkStats("bulk.multiple_users2").map(ctx.append(2, _))

    val response = agent.on(UserAction(List(1, 2)))
    if (response.isEmpty) fail("should have detected bulk") else {
      val signal = response.head.asInstanceOf[SketchySignal]
      assert(signal.strength === 0.8060132942205083)
      assert(signal.items.toSet === bulk)
    }
  }

  it should "NOT detect bulk for broadcasts of different kind" in {
    bulkStats("bulk.negative").map(ctx.append(1, _))

    val response = agent.on(UserAction(List(1)))
    if (!response.isEmpty) fail("should NOT have detected bulk")
  }

}
