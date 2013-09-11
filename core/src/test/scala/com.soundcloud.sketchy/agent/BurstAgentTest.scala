package com.soundcloud.sketchy.agent

import org.scalatest.FlatSpec
import java.util.Date

import com.soundcloud.sketchy.agent.limits._
import com.soundcloud.sketchy.context.{ Context, MemoryContext }
import com.soundcloud.sketchy.events._

import com.soundcloud.sketchy.SpecHelper

/**
 * Tests for Burst Agent
 */
class RateLimiterAgentTest extends FlatSpec with SpecHelper {
  behavior of "The burst agent"

  it should "emit signal after spam limit is exceeded for different types" in {
    new LimitsAllFeatures {
      oneMaxAssert(agent, "affiliation", aFollowsB, aFollowsB)
      oneMaxAssert(agent, "favoriting", aFavoritesB, aFavoritesB)
    }
  }

  it should "sum features grouped in a limit" in {
    new LimitsAllFeatures {
      oneMaxAssert(agent, "summed features", aFollowsB, aUnfollowsB)
    }
  }

  it should "generate limits with the correct feature symbols" in {
    new LimitsAllFeatures {
      assert(Set(
          "Affiliation:Create",   "Affiliation:Destroy",
          "Affiliation:Link",     "Affiliation:Relink",
          "Affiliation:Backlink", "Affiliation:Unlink").map(Symbol.apply)
          === agent.counterNames(defaultLimits.limits(0)).toSet)
    }
  }

  it should "generate signals that specify the specific limit broken" in {
    new LimitsAllFeatures {
      agent.on(aFollowsB)
      agent.on(aFollowsB).head match {
        case result: SketchySignal =>
          assert(result.detector === "Burst_Create+Destroy+Link+Relink+Backlink+Unlink_24.00hrs")
        case _ => fail("Burst agent should have emited a signal.")
      }
    }
  }

  it should "broken limits should clear their feature counters and not those of other limits" in {
    new LimitsPartiallyOverlappingFeatures {
      agent.on(aFollowsB)
      checkState(1, 0, 0)
      agent.on(aRefollowsB)

      checkState(1, 0, 1)
      agent.on(aUnfollowsB)
      checkState(0, 0, 1)

      def checkState(numFollows: Int, numUnfollows: Int, numRefollows: Int) {
        assert(ctx.counter(aFollowsB.sourceId,   agent.counterName(aFollowsB)) === numFollows)
        assert(ctx.counter(aUnfollowsB.sourceId, agent.counterName(aUnfollowsB)) === numUnfollows)
        assert(ctx.counter(aRefollowsB.sourceId, agent.counterName(aRefollowsB)) === numRefollows)
      }
    }
  }

  /**
   * Internal Spec Helpers
   */
  trait LimitsAllFeatures extends Fixtures {
    val defaultLimits = new BurstLimits(List("Affiliation","Favoriting").map(
      new BurstLimit(
        _, // actionKind
        features = List(UserEvent.Create,
          UserEvent.Destroy,
          EdgeChange.Link,
          EdgeChange.Relink,
          EdgeChange.Backlink,
          EdgeChange.Unlink),
        timeInterval = 24 * 60 * 60,
        max = 1)
    ))
    val ctx = countingContext()
    val agent = new RateLimiterAgent(ctx, defaultLimits)
  }

  trait LimitsPartiallyOverlappingFeatures extends Fixtures {
    val defaultLimits = new BurstLimits(List(
      new BurstLimit(
        actionKind = "Affiliation",
        features = List(EdgeChange.Link, EdgeChange.Unlink),
        timeInterval = 24 * 60 * 60,
        max = 1),
      new BurstLimit(
        actionKind = "Affiliation",
        features = List(EdgeChange.Unlink, EdgeChange.Relink),
        timeInterval = 24 * 60 * 60,
        max = 1)
      )
    )
    val ctx = countingContext()
    val agent = new RateLimiterAgent(ctx, defaultLimits)
  }

  trait Fixtures {
    val aFollowsB     = createAffiliationEdgeLike(EdgeChange.Link)
    val aUnfollowsB   = createAffiliationEdgeLike(EdgeChange.Unlink)
    val aRefollowsB   = createAffiliationEdgeLike(EdgeChange.Relink)
    val aBackfollowsB = createAffiliationEdgeLike(EdgeChange.Backlink)

    val aFavoritesB = edgeLikeUserToItem(1, 2, 3, "Favoriting", UserEvent.Create)
    val aUnfavoritesB = edgeLikeUserToItem(1, 2, 3, "Favoriting", UserEvent.Destroy)
  }


  def createAffiliationEdgeLike(inEdgeType: EdgeChange.Type): EdgeChange =
    new EdgeChange(
      sourceId = 1,
      sinkId = 2,
      ownerId = Some(1),
      actionKind = "Affiliation",
      edgeType = inEdgeType,
      createdAt = new Date())

  def oneMaxAssert(
    agent: RateLimiterAgent,
    spamKind: String,
    action1: Event,
    action2: Event) {

    burst_assert(agent.on(action1), 0.0, "should not have burst on first %s spam".format(spamKind))
    burst_assert(agent.on(action2), 1.0, "should have burst on %s spam".format(spamKind))
  }

  def burst_assert(
    output: Seq[com.soundcloud.sketchy.events.Event],
    sig: Double,
    failMsg: String) =

    output.headOption match {
      case Some(signal: SketchySignal) =>
        assert(signal.strength === sig)
      case _ =>
        if (sig == 0.0) assert(true) else fail(failMsg)
    }
}
