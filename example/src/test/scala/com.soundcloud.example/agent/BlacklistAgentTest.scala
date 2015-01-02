package com.soundcloud.example.agent

import org.scalatest.FlatSpec
import java.util.Date

import java.util.Date
import org.scalatest.FlatSpec

import com.soundcloud.sketchy.events.SketchySignal

import com.soundcloud.example.events.User
import com.soundcloud.example.SpecHelper


class BlacklistAgentTest extends FlatSpec with SpecHelper {

  behavior of "the BlacklistAgent"

  val user = User(Some(1), None, None, None, None, None, None, None,
    Some(List("192.168.1.1")), None, None)

  it should "emit a sketchy signal given a user with blacklisted IPs" in {
    val agent = blacklistAgent("192.168.1.1")
    val result = agent.on(user)
    assert(result.length === 1)
    result.head match {
      case sig: SketchySignal => assert(sig.userId === 1)
      case _ => fail("should have emitted a sketchy signal")
    }
  }

  it should "not emit a sketchy signal given a user with unlisted IPs" in {
    val agent = blacklistAgent("192.168.1.2")
    val result = agent.on(user)
    assert(result.isEmpty)
  }
}
