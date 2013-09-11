package com.soundcloud.sketchy.network

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events.Event

/**
 * Objects that can send events to agents
 */
trait Notifying {
  var agents: List[Agent] = Nil

  def ->(agent: Agent) = agents = agent :: agents
  def emit(output: Event) { agents.map(_.on(output)) }
}
