package com.soundcloud.sketchy.util

import com.soundcloud.sketchy.monitoring.Instrumented


object Exceptions extends Instrumented {

  def metricsSubtypeName: Option[String] = Some("exceptions")
  def metricsTypeName: String = "exceptions"

  def report(e: Throwable) {
    val exceptionType = e.toString.split(':').head
     counter
       .labels(exceptionType)
       .inc()
  }

  private val counter = prometheusCounter("exception", List("exception"))
}
