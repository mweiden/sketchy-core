package com.soundcloud.sketchy.agent

import org.scalatest.FlatSpec
import scala.actors.Actor.State._

import com.soundcloud.sketchy.SpecHelper
import com.soundcloud.sketchy.events.Event

/**
 * Agent test
 */
class AgentTest extends FlatSpec with SpecHelper {
  behavior of "A sketchy agent"

  val agent = new Agent() {
    def on(event: Event) = { throw new RuntimeException() }
  }

  it should "not propagate exceptions" in {

    agent.!?(1000, new Event() { val id = Some(1) })
    assert(agent.getState != Terminated)
  }

  it should "generate formatted metrics group name" in {
    assert(agent.metricsGroupName === "sketchy.test.agent")
    assert(agent.metricsName === "test_agent_total")
    assert(agent.timerName === "test_agent_timer")
    assert(agent.metricsTypeName === "Agent")
  }
}
