package com.soundcloud.sketchy.context

import org.scalatest.FlatSpec

import com.soundcloud.sketchy.events.UserEventKey

import com.soundcloud.sketchy.SpecHelper
import java.util.Date

/**
 * Check marshalling/unmarshalling of statistics
 */
class StatisticsTest extends FlatSpec with SpecHelper {
  behavior of "The statistics"

  it should "be correctly marshalled and unmarshalled" in {
    val junkStat = JunkStatistics(UserEventKey("Message", 1), 1.0)
    assert(JunkStatistics.unmarshal(junkStat.marshalled) === junkStat)

    val bulkStat = BulkStatistics(UserEventKey("Message", 1), List(1))
    assert(BulkStatistics.unmarshal(bulkStat.marshalled) === bulkStat)

    val ratioStat = RatioStatistics(UserEventKey("Message", 1), 100)
    assert(RatioStatistics.unmarshal(ratioStat.marshalled) === ratioStat)
  }
}
