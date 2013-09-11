package com.soundcloud.network.agent

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }

import com.soundcloud.sketchy.events.{
  SketchySignal,
  UserAction,
  UserEventKey
}
import com.soundcloud.sketchy.context.{
  Context,
  MemoryContext,
  BulkStatistics
}
import com.soundcloud.sketchy.agent.BulkStatisticsAgent

import com.soundcloud.sketchy.SpecHelper

/**
 * Update tests for BulkDetector
 */
class BulkStatisticsAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {
  behavior of "The bulk statistics agent"

  var ctx: MemoryContext[BulkStatistics] = _
  var agent: BulkStatisticsAgent = _
  var agentNoFingerprint: BulkStatisticsAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[BulkStatistics]()
    agent = new BulkStatisticsAgent(
      ctx,
      tokenizer(
        distSays = 2.0,
        featurizeSays = List(1,2),
        fingerprintSays = List(3,4))
      )
    agentNoFingerprint = new BulkStatisticsAgent(
      ctx,
      tokenizer(
        distSays = 2.0,
        featurizeSays = List(1,2),
        fingerprintSays = List())
      )
  }

  it should "signal user action for userId 1 message" in {
    agent.on(messageOutOfDict).headOption match {
      case Some(x: UserAction) => assert(x.userIds === List(1))
      case _ => fail("did not process userId 1 message")
    }
  }

  it should "signal user action for userId 1 comment" in {
    agent.on(commentCreated).headOption match {
      case Some(x: UserAction) => assert(x.userIds === List(1))
      case _ => fail("did not process userId 1 comment")
    }
  }

  it should "append message fingerprints into the context" in {
    agent.on(messageOutOfDict)

    val fingerprints = List(3,4)
    val expected = new BulkStatistics(UserEventKey("Message", 100), fingerprints)

    assert(ctx.get(1)(0) === expected)
  }

  it should "append comment fingerprints into the context" in {
    agent.on(commentCreated)

    val fingerprints = List(3,4)
    val expected = new BulkStatistics(UserEventKey("Comment", 55023212), fingerprints)

    assert(ctx.get(1)(0) === expected)
  }

  it should "discard statistics with no fingerprints" in {
    agentNoFingerprint.on(commentUnfingerprintable)

    assert(ctx.get(1) === Nil)
  }
}

