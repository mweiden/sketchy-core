package com.soundcloud.sketchy.util

import System.{ currentTimeMillis => now }


class Digest(limit: Int = 3, intervalTimeout: Long = 60L * 60L * 1000L) {
  import CircuitBreaker._

  protected var countThisInterval = Map[String,Int]()
  protected var thisIntervalTimestamp = now

  def allow(identifier: String) = this.synchronized {
    // clear the interval if it is old
    val age = now - thisIntervalTimestamp

    if (age >= intervalTimeout) {
      countThisInterval = Map[String,Int]()
      thisIntervalTimestamp = now
    }

    // see if the current request passes
    val countOpt = countThisInterval.get(identifier)

    val count = if (countOpt.isDefined) {
      countThisInterval += identifier -> (countOpt.get + 1)
      countThisInterval(identifier)
    } else {
      countThisInterval += identifier -> 0
      0
    }

    if (count < limit) true else false
  }
}
