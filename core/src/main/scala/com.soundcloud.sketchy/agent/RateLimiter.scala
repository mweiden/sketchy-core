package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.agent.limits.{Limit, Limits}
import com.soundcloud.sketchy.context.Context
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.monitoring.Prometheus

object RateLimiterAgent {
  protected val counter = Prometheus.counter(
    "ratelimits",
    "User action rate limits.",
    List("action", "limit"))
}

class RateLimiterAgent(counters: Context[Nothing], limits: Limits)
  extends Agent{
  import RateLimiterAgent._

  def on(event: Event): Seq[Event] = {
    event match {
      case event: UserEvent => {
        val uid = event.senderId.get

        // saw it
        val countAtIncrement = counters.increment(uid, counterName(event))

        // check for violated limit
        val violation = limits.filter(event).limits.find(l => l.doesBreak(count(l, uid)))

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
            "Rate_%s".format(limit.description),
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
   * @return the current rate count for the given limit and user
   */
  def count(limit: Limit, uid: Long): Long = {
    counterNames(limit).map(name =>
      counters.counter(uid, name, Some(limit.timeInterval * 1000L))
    ).sum
  }

  /**
   * @param limit an arbitrary rate limit
   * @return a list of rate count counter names relevant to the limit
   */
  def counterNames(limit: Limit): List[Symbol] =
    limit.features.map(f => Symbol(List(limit.actionKind, f).mkString(":")))

  /**
   * @param event an arbitrary user event
   * @return rate count counter name relevant to the user event
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

  private def meter(kind: String, limit: String) {
    counter.labels(kind, limit).inc
  }
}
