package com.soundcloud.sketchy.network

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events.Event

/**
 * Directly invoke all registered Agents with processing results
 */
trait Propagation extends Agent {

  def propagate(output: Seq[Event])

  abstract override def on(event: Event): Seq[Event] = {
    counter.newPartial()
      .labelPair("direction", "incoming")
      .labelPair("agent", metricsTypeName)
      .labelPair("kind", event.kind)
      .apply().increment()

    val output: Seq[Event] = timer {
      super.on(event)
    }

    counter.newPartial()
      .labelPair("direction", "outgoing")
      .labelPair("agent", metricsTypeName)
      .labelPair("kind", event.kind)
      .apply().increment(output.length)

    propagate(output)
    return output
  }

  private val counter = prometheusCounter("direction", "agent", "kind")
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
    output.map(event => agents.map(_ ! event))
  }

  override def enable(): Boolean = {
    start()
    true
  }
}

