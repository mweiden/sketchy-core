package com.soundcloud.sketchy.util

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }
import com.soundcloud.sketchy.SpecHelper

import scala.slick.lifted.Query
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.dynamicSession
import scala.slick.driver.MySQLDriver.backend.{ Database => SlickDatabase }

import java.sql.SQLException

/**
 * Database Test
 */
class DatabaseTest extends FlatSpec with SpecHelper {
  behavior of "database exception handling"

  val h2 = new H2Driver(h2db("sketchy.h2"))

  it should "failover to a secondary database should the first fail" in {
    val cfgs = List(
      new DatabaseCfg("bogus", "a", "b", "c", "d", h2),
      new DatabaseCfg("valid", "e", "f", "g", "h", h2))

    val db = new Database(List(cfgs.head))
    val failoverDB = new Database(cfgs)

    def doQuery(itIs: Boolean) = if (itIs) true else throw new SQLException("")

    assert(db.withFailover("test1", false, true){ doQuery(false) } === None)
    assert(db.withFailover("test2", false, true){ doQuery(true) } === Some(true))

    val inputs = List(true, false)
    var count = -1
    assert(failoverDB.withFailover("test3", false){
      count += 1; doQuery(inputs(count)) }.get === true)
  }

  it should "fail over to writeable hosts" in {
    val cfgs = List(
      new DatabaseCfg("RO", "i", "j", "k", "l", h2, readOnly = true),
      new DatabaseCfg("RW", "m", "n", "o", "p", h2, readOnly = false))

    val badDB = new Database(List(cfgs.head))
    val goodDB = new Database(List(cfgs.last))
    val failoverDB = new Database(cfgs)

    def doQuery(itIs: Boolean) = if (itIs) true else throw new SQLException("")

    assert(badDB.withFailover("test4", true, true){ doQuery(false) } === None)
    assert(goodDB.withFailover("test5", true, true){ doQuery(true) } === Some(true))

    val inputs = List(true, false)
    var count = -1
    assert(failoverDB.withFailover("test6", true, true ) {
     count += 1; doQuery(inputs(count)) } === Some(true))
  }

}


class TestDatabaseTest extends FlatSpec with SpecHelper {

  behavior of "the test database"

  val testRows = TableQuery[TestRows]

  it should "allow access to the h2 database" in {
    val db = database()

    assert(db.withFailover("test", true) {
      testRows.insert(TestRow(99, "a"))
    } === Some(1))

    assert(db.withFailover("test", true) {
      testRows.filter(row => row.id === 99).list
    } === Some(List(TestRow(99, "a"))))
  }

  it should "clear the h2 database after the control block has be executed" in {
    val db = database()
    assert(db.withFailover("test", false) {
      testRows.filter(row => row.id === 99).list
    } === Some(List()))
  }


  case class TestRow(id: Int, value: String)

  class TestRows(tag: Tag) extends Table[TestRow](tag, "test_table") {
    def id = column[Int]("id", O.PrimaryKey)
    def value = column[String]("value")
    def * = (id, value) <> (TestRow.tupled, TestRow.unapply)
  }
}

class DatabaseHealthMonitorTest
  extends FlatSpec with SpecHelper with BeforeAndAfterEach {

  var monitor: MockMonitor = _

  override def beforeEach() {
    monitor = new MockMonitor
    monitor.clean
  }

  behavior of "the isHealthy method"

  it should "query the health of the database if it is not stored" in {
    val cfg = dbCfg()
    assert(monitor.isHealthy(cfg.register))
  }

  it should "return a stored status if the status is cached" in {
    val cfg = dbCfg()
    val db = cfg.register
    monitor.setHealth(db, false)
    assert(!monitor.isHealthy(db))
  }

  it should "return false if the database is unhealthy" in {
    val cfg = dbCfg()
    val db = cfg.register
    val stubbedMonitor = new MockMonitor {
      override protected def queryHealth(db: SlickDatabase): Boolean = false
    }
    assert(!stubbedMonitor.isHealthy(db))
  }


  behavior of "the monitor object"

  it should "run an actor that checks database health on a timer" in {
    val cfg = dbCfg()
    val slick = cfg.register

    monitor.setHealth(slick, false)
    assert(!monitor.isHealthy(slick))
    monitor.update
    assert(monitor.isHealthy(slick))
  }

  it should "be able to differentiate between different database connections" in {
    val cfgA = DatabaseCfg(
      "sketchy",
      "",
      "",
      "127.0.0.1",
      "sketchy_production",
      new H2Driver(h2db("sketchy.h2")),
      readOnly = false)

    val cfgB = cfgA.copy(readOnly = true)

    val dbA = cfgA.register
    val dbB = cfgB.register

    monitor.setHealth(dbA, true)
    monitor.setHealth(dbB, false)
    assert(monitor.isHealthy(dbA) === true)
    assert(monitor.isHealthy(dbB) === false)
  }

  class MockMonitor extends DatabaseHealthMonitor {
    def update { Monitor.update }
    def setHealth(db: SlickDatabase, status: Boolean) {
      set(db, status)
    }
    def clean { clear }
  }


}
