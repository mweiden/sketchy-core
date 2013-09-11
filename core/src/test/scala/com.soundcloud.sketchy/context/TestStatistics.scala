package com.soundcloud.sketchy.context

import com.soundcloud.sketchy.events.UserEventKey

case class TestStatistics(id: Int) extends Statistics {
  def key: UserEventKey = UserEventKey("test", id)
  def marshalled: String = "test|" + id
}

object TestStatistics {
  def unmarshal(statistics: String): TestStatistics = {
    val Array(sKind, id) = statistics.split('|')

    TestStatistics(id.toInt)
  }
}
