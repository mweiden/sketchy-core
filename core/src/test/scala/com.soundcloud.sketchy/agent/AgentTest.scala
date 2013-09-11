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

  it should "not propagate exceptions" in {
    val agent = new Agent() {
      def on(event: Event) = { throw new RuntimeException() }
    }

    agent.!?(1000, new Event() { val id = Some(1) })
    assert(agent.getState != Terminated)
  }

}
