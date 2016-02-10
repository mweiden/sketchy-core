package com.soundcloud.sketchy.util

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }
import com.soundcloud.sketchy.SpecHelper

import scala.slick.driver.MySQLDriver.backend.{ Database => SlickDatabase }

import java.util.Date

class CircuitBreakerTest extends FlatSpec with BeforeAndAfterEach {
  import CircuitBreaker.{ Status, Retry }

  val toMonitor = Breakable("name", 'Value)

  var breaker: MockBreaker = _

  override def beforeEach() {
    breaker = new MockBreaker
    breaker.clean
  }

  behavior of "the update status method"

  it should "given an active status, store an active status" in {
    breaker.updateStatus(toMonitor, true)
    assert(breaker.get(toMonitor).isActive === true)
    assert(breaker.get(toMonitor).retryInfo === None)
  }

  it should "given an inactive status, store an inactive status and retry timer" in {
    breaker.updateStatus(toMonitor, false)
    assert(breaker.get(toMonitor).isActive === false)
    assert(breaker.get(toMonitor).retryInfo.get.retryDate === new Date(176523L))
    assert(breaker.get(toMonitor).retryInfo.get.retries === 1)
  }

  it should "given an inactive status, retreive and update an existing retry timer" in {
    breaker.set(toMonitor, Status(false, Some(Retry(new Date(0L)))))
    breaker.updateStatus(toMonitor, false)
    assert(breaker.get(toMonitor).isActive === false)
    assert(breaker.get(toMonitor).retryInfo.get.retryDate === new Date(285242L))
    assert(breaker.get(toMonitor).retryInfo.get.retries === 2)
  }

  behavior of "the is active meathod"

  it should "given a new object, return true and store an active status" in {
    assert(breaker.isActive(toMonitor))
    assert(breaker.get(toMonitor).isActive === true)
    assert(breaker.get(toMonitor).retryInfo === None)
  }

  it should "given a known object stored as active, return true" in {
    breaker.set(toMonitor, Status(true, None))
    assert(breaker.get(toMonitor).isActive === true)
  }

  it should "given a known object stored with unexpired retry timer, return false" in {
    breaker.set(
      toMonitor,
      Status(false, Some(Retry(new Date((new Date).getTime + 500000L)))))
    assert(breaker.isActive(toMonitor) === false)
  }

  it should "given a known object stored with expired retry timer, return true" in {
    breaker.set(
      toMonitor,
      Status(false, Some(Retry(new Date((new Date).getTime - 500000L)))))
    assert(breaker.isActive(toMonitor) === true)
  }


  behavior of "the status map"

  case class Breakable(name: String, value: Any)

  it should "map different objects to different statuses" in {
    val a = new Breakable("sketchy", 1)
    val b = new Breakable("sketchy", 2)
    breaker.set(a, Status(false, Some(Retry(new Date))))
    breaker.set(b, Status(true, None))
    assert(breaker.isActive(a) === false)
    assert(breaker.isActive(b) === true)
  }

  class MockBreaker extends CircuitBreaker {
    def get(b: Breakable) = status(b)
    def set(b: Breakable, sts: CircuitBreaker.Status) { status(b) = sts }
    def clean { clear }

    override protected def currentTime = new Date(0L)
  }
}
