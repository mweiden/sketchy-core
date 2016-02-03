package com.soundcloud.sketchy.network

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events.Event
import com.soundcloud.sketchy.monitoring.Prometheus

/**
 * Directly invoke all registered Agents with processing results
 */
object Propagation {
  protected val counter = Prometheus.counter("agent",
                                             "agent propagation metrics",
                                             List("direction", "agent", "kind"))
  protected val timer   = Prometheus.timer("agent",
                                           "agent timing metrics",
                                           List("agent", "kind"))
}

trait Propagation extends Agent {
  import Propagation._

  def propagate(output: Seq[Event])

  abstract override def on(event: Event): Seq[Event] = {
    counter
      .labels("incoming", metricsSubtypeName, event.kind)
      .inc()

    val output: Seq[Event] = timer.time(metricsSubtypeName, event.kind) {
      super.on(event)
    }

    output.foreach{ e =>
      counter
        .labels("outgoing", metricsSubtypeName, e.kind)
        .inc()
    }

    propagate(output)
    return output
  }

}

/**
 * Recurse subroutines on registered agents
 */
trait DirectPropagation extends Propagation {
  def propagate(output: Seq[Event]) { output.map((e: Event) => emit(Some(e))) }
}

/**
 * Send messages to registered agents
 */
trait ActorPropagation extends Propagation {
  def propagate(output: Seq[Event]) {
    output.par.map(event => agents.map(_ ! event))
  }

  override def enable(): Boolean = {
    start()
    true
  }
}

