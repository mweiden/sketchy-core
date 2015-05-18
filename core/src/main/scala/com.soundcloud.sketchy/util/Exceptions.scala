package com.soundcloud.sketchy.util

import com.soundcloud.sketchy.monitoring.{Instrumented, Prometheus}


object Exceptions extends Instrumented {

  val metricsName: String = "exceptions"

  def report(e: Throwable) {
    val exceptionType = e.toString.split(':').head
     counter
       .labels(exceptionType)
       .inc()
  }

  private val counter = Prometheus.counter(metricsName,
                                           "exceptions counter",
                                           List("exception"))
}
