package com.soundcloud.sketchy.network

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events.Event

/**
 * Directly invoke all registered Agents with processing results
 */
trait Propagation extends Agent {

  def propagate(output: Seq[Event])

  abstract override def on(event: Event): Seq[Event] = {
    val partial = counter.newPartial()
      .labelPair("direction", "incoming")

    val output: Seq[Event] = timer {
      super.on(event)
    }

    partial.labelPair("direction", "outgoing")
      .apply().increment()

    propagate(output)
    return output
  }

  private val counter = prometheusCounter("direction")
}

/**
 * Recurse subroutines on registered agents
 */
trait DirectPropagation extends Propagation {
  def propagate(output: Seq[Event]) { output.map(emit(_)) }
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

