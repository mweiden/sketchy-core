package com.soundcloud.sketchy.access

import java.text.SimpleDateFormat
import org.scalatest.{ BeforeAndAfterEach, FlatSpec }

import com.soundcloud.sketchy.SpecHelper
import com.soundcloud.sketchy.util.Database
import com.soundcloud.sketchy.events.{
  SketchyScore,
  SketchySignal,
  SketchyItem
}


/**
 * sketchy access test
 */
class SketchyAccessTest
  extends FlatSpec with SpecHelper with BeforeAndAfterEach {

  var db: Database = _
  var access: SketchyAccess = _

  override def beforeEach() {
    db = database()
    access = new SketchyAccess(db)
  }

  behavior of "sketchy access"

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val created = dateFormatter.parse("2013-07-21 07:00:00")
  val last = dateFormatter.parse("2013-07-21 08:00:00")

  it should "select a sketchy score given user id and kind" in {
    assert(access.select(1, "Test") === Some(SketchyScore(
      1, "Test", 2, 3, 4.0, 1.0, last, created)))
  }

  it should "insert a sketchy score given a sketchy signal" in {
    val sig = SketchySignal(2, "Test", List(11,12), "Detector", 1.0, created)
    assert(access.insert(sig, false))
    assert(access.select(2,"Test") === Some(SketchyScore(
      2, "Test", 1, 0, 1.0, 0.5263157894736842, created, created)))
  }

  it should "merge in sketchy items on sketchy score insertion" in {
    val sig = SketchySignal(3, "Test", List(21,22), "Detector", 1.0, created)
    assert(access.insert(sig))
    assert(access.selectItem("Test", 21) === Some(SketchyItem(21, "Test", created)))
    assert(access.selectItem("Test", 22) === Some(SketchyItem(22, "Test", created)))
  }

  it should "update a sketchy sketchy score given a sketchy signal" in {
    val sig = SketchySignal(4, "Test", List(31,32), "Detector", 1.0, created)
    val sig2 = sig.copy(createdAt = last)
    assert(access.insert(sig))
    assert(access.update(sig2))
    assert(access.select(4, "Test").get.lastSignaledAt === last)
  }

  it should "merge in new sketchy items on a sketchy score update" in {
    val sig = SketchySignal(5, "Test", List(41,42), "Detector", 1.0, created)
    val sig2 = sig.copy(createdAt = last, items = List(43))
    assert(access.insert(sig))
    assert(access.update(sig2))
    assert(access.selectItem("Test", 43) === Some(SketchyItem(43, "Test", last)))
  }

  it should "check if a user is trusted" in {
    assert(access.trusted(1))
    assert(!access.trusted(0))
  }

  it should "check if an item is sketchy" in {
    assert(access.sketchy("Test", 1))
    assert(!access.sketchy("Test", 0))
  }

  it should "select a sketchy item" in {
    assert(access.selectItem("Test", 1) === Some(SketchyItem(1, "Test", last)))
  }
}

