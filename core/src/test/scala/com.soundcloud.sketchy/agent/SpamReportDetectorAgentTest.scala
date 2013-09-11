package com.soundcloud.sketchy.agent

import java.text.SimpleDateFormat
import java.util.Date

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterEach

import com.soundcloud.sketchy.context.{ MemoryContext, SpamReportStatistics }
import com.soundcloud.sketchy.events._

import com.soundcloud.sketchy.SpecHelper

/**
 * Detector tests for SpamReportDetector
 */
class SpamReportDetectorAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {

  behavior of "The spam report detector agent"

  var ctx: MemoryContext[SpamReportStatistics] = _
  var agent: SpamReportDetectorAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[SpamReportStatistics]()
    agent = new SpamReportDetectorAgent(ctx)
  }

  val fmt = "yyyy/MM/dd HH:mm:ss ZZZZZ"
  val dateFormatter = new SimpleDateFormat(fmt)

  it should "NOT signal for less than 3 reporters and less than 10 reports" in {
    val keys = List(
      UserEventKey("SpamReport", 123),
      UserEventKey("SpamReport", 456))

    ctx.append(12, SpamReportStatistics(keys(0), 42, "Message",
      dateFormatter.parse("2012/05/29 00:37:11 +0000"), new Date(0)))
    ctx.append(12, SpamReportStatistics(keys(1), 676, "Comment",
      dateFormatter.parse("2012/06/05 06:12:45 +0000"), new Date(0)))

    agent.on(UserAction(List(12))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected reported spam")
      case _ =>
    }

    (1 to 7).foreach(id => ctx.append(12, SpamReportStatistics(keys(1), 676,
      "Comment", dateFormatter.parse("2012/06/05 06:12:45 +0000"), new Date(0))))

    agent.on(UserAction(List(12))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected reported spam")
      case _ =>
    }
  }

  it should "signal for 3 or more different reporters" in {
    val keys = List(
      UserEventKey("SpamReport", 123),
      UserEventKey("SpamReport", 456),
      UserEventKey("SpamReport", 41))

    ctx.append(12, SpamReportStatistics(keys(0), 42, "Comment",
      dateFormatter.parse("2012/05/29 00:37:11 +0000"), new Date(0)))
    ctx.append(12, SpamReportStatistics(keys(1), 676, "Comment",
      dateFormatter.parse("2012/06/05 06:12:45 +0000"), new Date(0)))
    ctx.append(12, SpamReportStatistics(keys(2), 22, "Comment",
      dateFormatter.parse("2012/06/11 02:52:22 +0000"), new Date(0)))

    agent.on(UserAction(List(12))).headOption match {
      case Some(signal: SketchySignal) =>
        assert(signal.items.toSet === Set(41, 123, 456))
      case _ =>
        fail("should have detected reported spam")
    }
  }

  it should "NOT signal for 3 different reporters but different reporting kinds" in {
    val keys = List(
      UserEventKey("SpamReport", 123),
      UserEventKey("SpamReport", 456),
      UserEventKey("SpamReport", 41))

    ctx.append(12, SpamReportStatistics(keys(0), 42, "Message",
      dateFormatter.parse("2012/05/29 00:37:11 +0000"), new Date(0)))
    ctx.append(12, SpamReportStatistics(keys(1), 676, "Message",
      dateFormatter.parse("2012/06/05 06:12:45 +0000"), new Date(0)))
    ctx.append(12, SpamReportStatistics(keys(2), 22, "Comment",
      dateFormatter.parse("2012/06/11 02:52:22 +0000"), new Date(0)))

    agent.on(UserAction(List(12))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected reported spam")
      case _ =>
    }
  }

  it should "signal for 10 or more reports" in {
    (1 to 10).foreach(id => ctx.append(12, SpamReportStatistics(
      UserEventKey("SpamReport", id), 676, "Message",
      dateFormatter.parse("2012/06/05 06:12:45 +0000"), new Date(0))))

    agent.on(UserAction(List(12))).headOption match {
      case Some(signal: SketchySignal) =>
        assert(signal.items.toSet === (1 to 10).toSet)
      case _ =>
        fail("should have detected reported spam")
    }
  }

  it should "filter outdated reports" in {
    val keys = List(
      UserEventKey("SpamReport", 1),
      UserEventKey("SpamReport", 2),
      UserEventKey("SpamReport", 3),
      UserEventKey("SpamReport", 4),
      UserEventKey("SpamReport", 5),
      UserEventKey("SpamReport", 6))

    // simulate:
    //   message spam sketchy signal at 2012/06/01 06:12:45 +0000
    //   message spam sketchy signal at 2012/06/02 23:34:09 +0000
    //   message spam sketchy signal at 2012/06/12 13:04:12 +0000

    ctx.append(12, SpamReportStatistics(keys(0), 42, "Message",
      dateFormatter.parse("2012/05/29 00:37:11 +0000"),
      new Date(0))) // filtered
    ctx.append(12, SpamReportStatistics(keys(1), 123, "Message",
      dateFormatter.parse("2012/07/02 18:00:51 +0000"),
      dateFormatter.parse("2012/06/12 13:04:12 +0000"))) // not filtered
    ctx.append(12, SpamReportStatistics(keys(2), 676, "Message",
      dateFormatter.parse("2012/06/13 06:12:45 +0000"),
      dateFormatter.parse("2012/06/12 13:04:12 +0000"))) // not filtered
    ctx.append(12, SpamReportStatistics(keys(3), 22, "Message",
      dateFormatter.parse("2012/06/01 12:11:50 +0000"),
      dateFormatter.parse("2012/06/01 06:12:45 +0000"))) // filtered
    ctx.append(12, SpamReportStatistics(keys(4), 89, "Message",
      dateFormatter.parse("2012/06/10 14:20:44 +0000"),
      dateFormatter.parse("2012/06/02 23:34:09 +0000"))) // filtered

    agent.on(UserAction(List(12))).headOption match {
      case Some(x: SketchySignal) => fail("should NOT have detected reported spam")
      case _ =>
    }

    ctx.append(12, SpamReportStatistics(keys(5), 90, "Message",
      dateFormatter.parse("2012/06/12 14:20:44 +0000"),
      dateFormatter.parse("2012/06/12 13:04:12 +0000"))) // not filtered

    agent.on(UserAction(List(12))).headOption match {
      case Some(signal: SketchySignal) =>
        assert(signal.items.toSet === Set(2, 3, 6))
      case _ =>
        fail("should have detected reported spam")
    }
  }

}
