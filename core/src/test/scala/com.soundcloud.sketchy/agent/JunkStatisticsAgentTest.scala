package com.soundcloud.sketchy.agent

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }

import com.soundcloud.sketchy.context.{ MemoryContext, JunkStatistics }
import com.soundcloud.sketchy.events._

import com.soundcloud.sketchy.SpecHelper

/**
 * Update tests for JunkDetector
 */
class JunkStatisticsAgentTest
  extends FlatSpec with SpecHelper with BeforeAndAfterEach {

  behavior of "The core junk statistics agent"

  var ctx: MemoryContext[JunkStatistics] = _
  var agent: JunkStatisticsAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[JunkStatistics]()
    agent = new JunkStatisticsAgent(ctx, classifier(predictSays = 1.0))
  }

  it should "signal user action for userId 1 on message" in {
    agent.on(messageOutOfDict).head match {
      case UserAction(userId) => assert(userId === List(1))
      case _ => fail("did not process userId 1")
    }
  }

  it should "signal user action for userId 1 on comment" in {
    agent.on(commentCreated).head match {
      case UserAction(userId) => assert(userId === List(1))
      case _ => fail("did not process userId 1")
    }
  }

  it should "append message spam(1) confidence into the context" in {
    agent.on(messageOutOfDict)

    val expected = new JunkStatistics(UserEventKey("Message", 100), 1.0)

    assert(ctx.get(1) === List(expected))
  }

  it should "append message spam(2) confidence into the context" in {
    agent.on(messageJunk)

    val expected = new JunkStatistics(UserEventKey("Message", 100), 1.0)

    assert(ctx.get(1) === List(expected))
  }

  it should "append comment spam(1) confidence into the context" in {
    agent.on(commentCreated)

    val expected = new JunkStatistics(UserEventKey("Comment", 55023212), 1.0)

    assert(ctx.get(1) === List(expected))
  }

  it should "append comment spam(2) confidence into the context" in {
    agent.on(commentJunk)

    val expected = new JunkStatistics(UserEventKey("Comment", 55023212), 1.0)

    assert(ctx.get(1) === List(expected))
  }

  it should "append comment spam(3) confidence into the context" in {
    agent.on(commentCreated)

    val expected = new JunkStatistics(UserEventKey("Comment", 55023212), 1.0)

    assert(ctx.get(1) === List(expected))
  }

  it should "NOT signal user action for too short content" in {
    assert(agent.on(messageShort).isEmpty)
  }

  it should "NOT signal user action for trusted comment" in {
    assert(agent.on(commentTrusted).isEmpty)
  }

}
