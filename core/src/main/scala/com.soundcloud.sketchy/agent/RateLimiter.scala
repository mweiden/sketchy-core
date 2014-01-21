package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.agent.limits.{ BurstLimit, BurstLimits }
import com.soundcloud.sketchy.context.Context

import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.Logging
import com.soundcloud.sketchy.monitoring.Prometheus


class RateLimiterAgent(counters: Context[Nothing], limits: BurstLimits)
  extends Agent with Logging {

  def on(event: Event): Seq[Event] = {
    event match {
      case event: UserEvent => {
        println(event)
        val uid = event.senderId.get

        // saw it
        val countAtIncrement = counters.increment(uid, counterName(event))

        // check for violated limit
        val violation = limits.filter(event).limits.find(l => count(l, uid) > l.max)

        // delete counters so that users start from zero again
        violation.foreach(l => counterNames(l).foreach(counters.deleteCounter(uid, _)))

        // graph it
        violation.foreach(l => meter(event.kind, l.description))

        // emit a signal for each violated limit
        violation.map(limit =>
          SketchySignal(
            uid,
            event.kind,
            Nil,
            "Burst_%s".format(limit.description),
            1.0,
            new Date())).toList
      }
      case _ => Nil
    }
  }

  /**
   * Counts across potentially many counters, depending on the limit.
   *
   * @param limit an arbitrary limit
   * @param uid user to check limit count for
   * @return the current burst count for the given limit and user
   */
  def count(limit: BurstLimit, uid: Int): Long = {
    counterNames(limit).map(name =>
      counters.counter(uid, name, Some(limit.timeInterval * 1000L))
    ).sum
  }

  /**
   * @param limit an arbitrary burst limit
   * @return a list of burst count counter names relevant to the limit
   */
  def counterNames(limit: BurstLimit): List[Symbol] =
    limit.features.map(f => Symbol(List(limit.actionKind, f).mkString(":")))

  /**
   * @param event an arbitrary user event
   * @return burst count counter name relevant to the user event
   */
  def counterName(event: UserEvent): Symbol = {
    val name = event match {
      case change: EdgeChange =>
        List(change.actionKind, change.edgeType).mkString(":")
      case _ =>
        List(event.kind, event.action).mkString(":")
    }

    Symbol(name)
  }

  private val counter = Prometheus.counter(
    "sketchy",
    "detection_ratelimits_total",
    "User action rate limits.",
    List("action", "limit"))

  private def meter(kind: String, limit: String) {
    counter.newPartial()
      .labelPair("action", kind)
      .labelPair("limit", limit)
      .apply().increment()
  }
}
