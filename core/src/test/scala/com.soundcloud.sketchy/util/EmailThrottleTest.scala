package com.soundcloud.sketchy.util

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }
import com.soundcloud.sketchy.SpecHelper

import scala.slick.driver.MySQLDriver.backend.{ Database => SlickDatabase }

import java.util.Date


class EmailThrottleTest extends FlatSpec {

  behavior of "the allow method"

  it should "allow a limited number of a given type" in {
    val throttle = new EmailThrottle()
    assert(throttle.allow(e1) === true)
    assert(throttle.allow(e1) === true)
    assert(throttle.allow(e1) === true)
    assert(throttle.allow(e1) === false)
    assert(throttle.allow(e1) === false)
  }

  it should "differentiate betweem different objects of the same type" in {
    val throttle = new EmailThrottle(limit = 1)
    assert(throttle.allow(e1) === true)
    assert(throttle.allow(e2) === true)
  }

  it should "clear the counter after the time interval timeout is up" in {
    val digest = new EmailThrottle(limit = 1, intervalTimeout = 0L)
    assert(digest.allow(e1) === true )
    assert(digest.allow(e1) === true )
  }

  val e1 = (new java.lang.RuntimeException("Oh noes, I ruined everything.")).toString
  val e2 = (new java.lang.RuntimeException("This time in a different way!")).toString
}
