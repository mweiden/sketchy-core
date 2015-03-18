package com.soundcloud.sketchy.util

import com.soundcloud.sketchy.monitoring.Instrumented


object Exceptions extends Instrumented {

  def metricsSubtypeName: Option[String] = Some("exceptions")
  def metricsTypeName: String = "exceptions"

  def report(e: Throwable) {
    val exceptionType = e.toString.split(':').head
     counter.newPartial()
       .labelPair("exception", exceptionType)
       .apply().increment()
  }

  private val counter = prometheusCounter("exception")
}
