package com.soundcloud.sketchy.network

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events.Event
import com.soundcloud.sketchy.monitoring.Prometheus

/**
 * Directly invoke all registered Agents with processing results
 */
trait Propagation extends Agent {

  def propagate(output: Seq[Event])

  abstract override def on(event: Event): Seq[Event] = {
    counter
      .labels("incoming", metricsSubtypeName.get, event.kind)
      .inc()

    val output: Seq[Event] = timer {
      super.on(event)
    }

    output.foreach{ e =>
      counter
        .labels("outgoing", metricsSubtypeName.get, e.kind)
        .inc()
    }

    propagate(output)
    return output
  }

  private val counter = Prometheus.counter("agent",
                                           "agent propagation metrics",
                                           List("direction", "agent", "kind"))
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

