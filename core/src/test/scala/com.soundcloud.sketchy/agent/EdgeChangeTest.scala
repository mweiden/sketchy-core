package com.soundcloud.sketchy.agent

import java.util.Date
import org.scalatest.FlatSpec
import com.soundcloud.sketchy.context.Context

import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.SpecHelper

/**
 * Update tests for EdgeChangeAgent
 */
class EdgeChangeAgentTest extends FlatSpec with SpecHelper {

  // some events are generally bidirectional, ie affiliation
  trait BiDirectional {
    val aFollowsB   = edgeLikeUserToUser(0, 1, 2, UserEvent.Create)
    val bFollowsA   = edgeLikeUserToUser(0, 2, 1, UserEvent.Create)
    val aUnfollowsB = edgeLikeUserToUser(0, 1, 2, UserEvent.Destroy)
    val bUnfollowsA = edgeLikeUserToUser(0, 2, 1, UserEvent.Destroy)

    val aFollowsBAdmin   = edgeLikeUserToUser(0, 1, 2, UserEvent.Create, true)

    val elCtx = countingContext()
    val agent = new EdgeChangeAgent(elCtx)
  }

  behavior of "EdgeChangeAgent bidirectional UserEvent.Create derived edge type"

  it should "given no previous action, create a link" in {
    new BiDirectional {
      val changes = agent.on(aFollowsB)

      assert(changes.size == 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
    }
  }

  it should "given last was an outbound destroy, create a relink" in {
    new BiDirectional {
      agent.on(aFollowsB)
      agent.on(aUnfollowsB)

      val changes = agent.on(aFollowsB)

      assert(changes.size == 2)
      assertChange(
        changes.head,
        EdgeChange.Relink,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
      assertChange(
        changes.last,
        EdgeChange.Link,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
    }
  }

  it should "given last was an inbound destroy, create a backlink" in {
    new BiDirectional {
      agent.on(aFollowsB)
      agent.on(aUnfollowsB)

      val changes =  agent.on(bFollowsA)

      assert(changes.size === 2)

      assertChange(
        changes.head,
        EdgeChange.Backlink,
        bFollowsA.senderId,
        bFollowsA.senderId,
        bFollowsA.recipientId
      )
      assertChange(
        changes.last,
        EdgeChange.Link,
        bFollowsA.senderId,
        bFollowsA.senderId,
        bFollowsA.recipientId
      )
    }
  }

  it should "given last was an inbound create, create a backlink" in {
    new BiDirectional {
      agent.on(aFollowsB)

      val changes =  agent.on(bFollowsA)

      assert(changes.size === 2)
      assertChange(
        changes.head,
        EdgeChange.Backlink,
        bFollowsA.senderId,
        bFollowsA.senderId,
        bFollowsA.recipientId
      )
      assertChange(
        changes.last,
        EdgeChange.Link,
        bFollowsA.senderId,
        bFollowsA.senderId,
        bFollowsA.recipientId
      )
    }
  }

  behavior of "EdgeChangeAgent bidirectional UserEvent.Destroy derived edge type"

  it should "given last was an outbound create, create an unlink" in {
    new BiDirectional {
      agent.on(aFollowsB)

      val changes = agent.on(aUnfollowsB)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Unlink,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
    }
  }

  it should "given last was an inbound create, create an unlink" in {
    new BiDirectional {
      agent.on(aFollowsB)
      agent.on(bFollowsA)

      val changes = agent.on(aUnfollowsB)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Unlink,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
    }
  }

  it should "given last was an inbound destroy, create an unlink" in {
    new BiDirectional {
      agent.on(aFollowsB)
      agent.on(bFollowsA)
      agent.on(bUnfollowsA)

      val changes = agent.on(aUnfollowsB)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Unlink,
        aFollowsB.senderId,
        aFollowsB.senderId,
        aFollowsB.recipientId
      )
    }
  }

  trait UniDirectional {
    val post0 = 0
    val post1 = 1
    val post8 = 8
    val post9 = 9

    val aLinksPost0ToPost9   = edgeLikeItemToItem(0, 1, post0, post9, UserEvent.Create)
    val aUnlinksPost0ToPost9 = edgeLikeItemToItem(1, 1, post0, post9, UserEvent.Destroy)

    val aLinksPost1ToPost9   = edgeLikeItemToItem(2, 1, post1, post9, UserEvent.Create)
    val aUnlinksPost1ToPost9 = edgeLikeItemToItem(3, 1, post1, post9, UserEvent.Destroy)

    val aLinksPost0ToPost8   = edgeLikeItemToItem(4, 1, post0, post8, UserEvent.Create)
    val bLinksPost0ToPost9   = edgeLikeItemToItem(5, 2, post0, post9, UserEvent.Create)

    val aFavoritesPost0     = edgeLikeUserToItem(0, 1, 0, "Favoriting", UserEvent.Create)
    val aUnfavoritesPost0   = edgeLikeUserToItem(0, 1, 0, "Favoriting", UserEvent.Update, new Date)
    val aRefavoritesPost0   = edgeLikeUserToItem(0, 1, 0, "Favoriting", UserEvent.Update)

    val aFavoritesCommentB = edgeLikeUserToItem(0, 1, 2, "Favoriting", UserEvent.Create)
    val aUnfavoritesCommentB = edgeLikeUserToItem(0, 1, 2, "Favoriting", UserEvent.Destroy)

    val elCtx = countingContext()
    val agent = new EdgeChangeAgent(elCtx)
  }

  behavior of "EdgeChangeAgent unidirectional UserEvent.Create derived edge type"

  it should "separate edge changes with a different source id" in {
    new UniDirectional {
      agent.on(aLinksPost0ToPost9)
      val changes = agent.on(aLinksPost1ToPost9)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        aLinksPost1ToPost9.senderId,
        Some(post1),
        Some(post9)
      )
    }
  }

  it should "separate edge changes with a different sink id" in {
    new UniDirectional {
      agent.on(aLinksPost0ToPost9)
      val changes = agent.on(aLinksPost0ToPost8)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        aLinksPost0ToPost8.senderId,
        Some(post0),
        Some(post8)
      )
    }
  }

  it should "separate edge changes with a different graph id" in {
    new UniDirectional {
      agent.on(aLinksPost0ToPost9)
      val changes = agent.on(bLinksPost0ToPost9)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        bLinksPost0ToPost9.senderId,
        Some(post0),
        Some(post9)
      )
    }
  }

  it should "given no previous action, create an link" in {
    new UniDirectional {
      val changes = agent.on(aLinksPost0ToPost9)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        aLinksPost0ToPost9.senderId,
        Some(post0),
        Some(post9)
      )
    }
  }

  it should "given last was an outbound destroy, create a relink" in {
    new UniDirectional {
      agent.on(aLinksPost0ToPost9)
      agent.on(aUnlinksPost0ToPost9)

      val changes = agent.on(aLinksPost0ToPost9)

      assert(changes.size === 2)
      assertChange(
        changes.head,
        EdgeChange.Relink,
        aLinksPost0ToPost9.senderId,
        Some(post0),
        Some(post9)
      )
      assertChange(
        changes.last,
        EdgeChange.Link,
        aLinksPost0ToPost9.senderId,
        Some(post0),
        Some(post9)
      )
    }
  }

  it should "create edge changes with the correct owner id" in {
    new UniDirectional {
      val changes = agent.on(aFavoritesCommentB)

      assert(changes.size === 1)
      changes.head match {
       case edge: EdgeChange => assert(edge.ownerId === aFavoritesCommentB.senderId)
       case _ => fail("Invalid output type from edge change agent.")
      }
    }
  }

  behavior of "EdgeChangeAgent unidirectional UserEvent.Destroy derived edge type"

  it should "given last was an outbound create, create an unlink" in {
    new UniDirectional {
      agent.on(aLinksPost0ToPost9)
      val changes = agent.on(aUnlinksPost0ToPost9)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Unlink,
        aLinksPost0ToPost9.senderId,
        Some(post0),
        Some(post9)
      )
    }
  }


  behavior of "EdgeChangeAgent unidirectional UserEvent.Update derived edge type"

  it should "given an outbound update without 'deleted at' information, create a link" in {
    new UniDirectional {
      val changes = agent.on(aRefavoritesPost0)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Link,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.itemId
      )
    }
  }

  it should "given an outbound update with 'deleted at' information, create an unlink" in {
    new UniDirectional {
      val changes = agent.on(aUnfavoritesPost0)

      assert(changes.size === 1)
      assertChange(
        changes.head,
        EdgeChange.Unlink,
        aUnfavoritesPost0.senderId,
        aUnfavoritesPost0.senderId,
        aUnfavoritesPost0.itemId
      )
    }
  }

  it should "given an update create and last was a update destroy, create an relink" in {
    new UniDirectional {
      agent.on(aFavoritesPost0)
      agent.on(aUnfavoritesPost0)
      val changes = agent.on(aRefavoritesPost0)

      assert(changes.size === 2)
      assertChange(
        changes.head,
        EdgeChange.Relink,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.itemId
      )
      assertChange(
        changes.last,
        EdgeChange.Link,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.senderId,
        aRefavoritesPost0.itemId
      )
    }
  }

  behavior of "EdgeChangeAgent event property handling"

  it should "not process events tagged as originating from admin" in {
    new BiDirectional {
      val changes = agent.on(aFollowsBAdmin)

      assert(changes === Nil)
    }
  }


  /**
   * Internal Spec Helpers
   */
  def assertChange(
    event: Event,
    edgeType: EdgeChange.Type,
    ownerId: Option[Long],
    sourceId: Option[Long],
    sinkId: Option[Long]) = {

    event match {
      case change: EdgeChange => {
        assert(edgeType === change.edgeType)
        assert(ownerId === change.senderId)
        assert(sourceId.get === change.sourceId)
        assert(sinkId.get === change.sinkId)
      }

      case _ => fail("Need EdgeChange result")
    }
  }

}
