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

  it should "generate a formatted metrics group name" in {
    val cfgs = List(new DatabaseCfg("bogus", "a", "b", "c", "d", h2))

    val db = new Database(cfgs)
    assert(db.metricsGroupName === "sketchy.test")
    assert(db.metricsTypeName === "bogus")
    assert(db.metricsName === "test_total")
    assert(db.timerName === "test_timer")
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

