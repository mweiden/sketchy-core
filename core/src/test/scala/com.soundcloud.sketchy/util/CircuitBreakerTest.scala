package com.soundcloud.sketchy.util

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }
import com.soundcloud.sketchy.SpecHelper

import scala.slick.driver.MySQLDriver.backend.{ Database => SlickDatabase }

import java.util.Date


class CircuitBreakerTest
  extends FlatSpec with SpecHelper with BeforeAndAfterEach {
  import CircuitBreaker.{ Status, Retry }

  val cfg = dbCfg()
  val db = cfg.register

  var breaker: MockBreaker = _

  override def beforeEach() {
    breaker = new MockBreaker
    breaker.clean
  }

  behavior of "the update status method"

  it should "given an active status, store an active status" in {
    breaker.updateStatus(db, true)
    assert(breaker.get(db).isActive === true)
    assert(breaker.get(db).retryInfo === None)
  }

  it should "given an inactive status, store an inactive status and retry timer" in {
    breaker.updateStatus(db, false)
    assert(breaker.get(db).isActive === false)
    assert(breaker.get(db).retryInfo.get.retryDate === new Date(176523L))
    assert(breaker.get(db).retryInfo.get.retries === 1)
  }

  it should "given an inactive status, retreive and update an existing retry timer" in {
    breaker.set(db, Status(false, Some(Retry(new Date(0L)))))
    breaker.updateStatus(db, false)
    assert(breaker.get(db).isActive === false)
    assert(breaker.get(db).retryInfo.get.retryDate === new Date(285242L))
    assert(breaker.get(db).retryInfo.get.retries === 2)
  }

  behavior of "the is active meathod"

  it should "given a new object, return true and store an active status" in {
    assert(breaker.isActive(db))
    assert(breaker.get(db).isActive === true)
    assert(breaker.get(db).retryInfo === None)
  }

  it should "given a known object stored as active, return true" in {
    breaker.set(db, Status(true, None))
    assert(breaker.get(db).isActive === true)
  }

  it should "given a known object stored with unexpired retry timer, return false" in {
    breaker.set(
      db,
      Status(false, Some(Retry(new Date((new Date).getTime + 500000L)))))
    assert(breaker.isActive(db) === false)
  }

  it should "given a known object stored with expired retry timer, return true" in {
    breaker.set(
      db,
      Status(false, Some(Retry(new Date((new Date).getTime - 500000L)))))
    assert(breaker.isActive(db) === true)
  }


  behavior of "the status map"

  it should "map different objects to different statuses" in {
    val cfgA = DatabaseCfg("sketchy", "", "", "", "", new MysqlDriver)
    val cfgB = DatabaseCfg("sketchy", "", "", "", "", new MysqlDriver, readOnly = true)
    val dbA = cfgA.register
    val dbB = cfgB.register
    breaker.set(dbA, Status(false, Some(Retry(new Date))))
    breaker.set(dbB, Status(true, None))
    assert(breaker.isActive(dbA) === false)
    assert(breaker.isActive(dbB) === true)
  }

  class MockBreaker extends CircuitBreaker {
    def get(db: SlickDatabase) = status(db)
    def set(db: SlickDatabase, sts: CircuitBreaker.Status) { status(db) = sts }
    def clean { clear }

    override protected def currentTime = new Date(0L)
  }
}
