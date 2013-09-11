package com.soundcloud.sketchy.util

import org.scalatest.FlatSpec
import com.soundcloud.sketchy.SpecHelper

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

    assert(db.withFailover("test1", false){ doQuery(false) } === None)
    assert(db.withFailover("test2", false){ doQuery(true) } === Some(true))

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

    assert(badDB.withFailover("test4", true){ doQuery(false) } === None)
    assert(goodDB.withFailover("test5", true){ doQuery(true) } === Some(true))

    val inputs = List(true, false)
    var count = -1
    assert(failoverDB.withFailover("test6", true){
     count += 1; doQuery(inputs(count)) } === Some(true))
  }

}
