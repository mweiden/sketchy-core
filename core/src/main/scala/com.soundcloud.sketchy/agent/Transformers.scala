package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.context.Context

/**
 * Emits EdgeChange events, given EdgeLike input events. EdgeChange events are
 * determined by users' past create and destroy behavior for a specific action
 * type.
 *
 * The table below displays the edgeTypes of the derived EdgeChange event
 * given the actions of Users A and B in a bi-direction edge relationship.
 *
 */
class EdgeChangeAgent(historyCtx: Context[Nothing]) extends Agent with Parsing {
  def on(event: Event): Seq[Event] = {
    event match {
      case edge: EdgeLike if !edge.noSpamCheck => dispatch(edge)
      case _ => return Nil
    }
  }

  protected def dispatch(edge: EdgeLike): Seq[Event] = {
    val edgeTypes = edge match {
      case e: EdgeLike if(edge.wasCreated) => enrichCreate(edge)
      case e: DeleteOnUpdate if (e.deletedAt == null) => enrichCreate(edge)
      case _ => List(EdgeChange.Unlink)
    }

    val (id, name) = counterName(edge, edge.wasCreated)
    val count = historyCtx.increment(id, name, edge.createdAt.getTime)

    edgeTypes.map( edgeType =>
      new EdgeChange(
        edge.sourceId,
        edge.sinkId,
        edge.senderId,
        edge.edgeKind,
        edgeType,
        edge.createdAt))
  }

  protected def counterName(
    edge: EdgeLike,
    isCreate: Boolean,
    isOutbound: Boolean = true): (Long, Symbol) = {

    val (sourceId, sinkId) = if (isOutbound) {
        (edge.sourceId, edge.sinkId)
      } else {
        (edge.sinkId, edge.sourceId)
      }

    (edge.graphId.getOrElse(0).toLong,
    Symbol(List(
      edge.kind,
      if (edge.wasCreated) "c" else "d",
      sourceId,
      sinkId).mkString(":"))
    )
  }

  protected def enrichCreate(edge: EdgeLike): List[EdgeChange.Type] = {
    if (eventCount(edge, isOutbound = true, isCreate = false) > 0L) {
      List(EdgeChange.Relink, EdgeChange.Link)
    } else if (
      edge.isBidirectional &&
      eventCount(edge, isOutbound = false, isCreate = true) > 0L) {
      List(EdgeChange.Backlink, EdgeChange.Link)
    } else {
      List(EdgeChange.Link)
    }
  }

  protected def eventCount(edge: EdgeLike, isOutbound: Boolean, isCreate: Boolean): Long = {
    val (id, name) = counterName(edge, isCreate, isOutbound)
    historyCtx.counter(id, name)
  }
}

