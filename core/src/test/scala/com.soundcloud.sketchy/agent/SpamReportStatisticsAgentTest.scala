package com.soundcloud.network.agent

import java.text.SimpleDateFormat
import java.util.Date
import org.scalatest.{ FlatSpec, BeforeAndAfterEach }

import com.soundcloud.sketchy.context.{ MemoryContext, SpamReportStatistics }
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.agent.SpamReportStatisticsAgent
import com.soundcloud.sketchy.SpecHelper
import com.soundcloud.sketchy.context.SpamReportStatistics

/**
 * Update tests for SpamReportDetector
 */
class SpamReportStatisticsAgentTest
  extends FlatSpec with BeforeAndAfterEach with SpecHelper {
  behavior of "The spam report statistics agent"

  val fmt = "yyyy/MM/dd HH:mm:ss ZZZZZ"
  val dateFormatter = new SimpleDateFormat(fmt)

  var ctx: MemoryContext[SpamReportStatistics] = _
  var agent: SpamReportStatisticsAgent = _

  override def beforeEach() {
    ctx = new MemoryContext[SpamReportStatistics]()
    agent = new SpamReportStatisticsAgent(ctx)
  }


  it should "signal user action for spammerId 23 on spam report" in {
    agent.on(spamReport("comment")).head match {
      case UserAction(spammerId) => assert(spammerId === List(23))
      case _ => fail("did not process userId 1")
    }
  }

  it should "append spam report context" in {
    agent.on(spamReport("comment"))

    val expected = SpamReportStatistics(UserEventKey("SpamReport", 16668), 42,
      "Comment", dateFormatter.parse("2012/05/29 00:37:11 +0000"), new Date(0))

    assert(ctx.get(23) === List(expected))
  }

  it should "NOT append spam report context for out-dated" in {
    agent.on(spamReport("enriched"))

    assert(ctx.get(23) === Nil)
  }

}
