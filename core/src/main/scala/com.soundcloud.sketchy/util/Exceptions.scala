package com.soundcloud.sketchy.util

import com.soundcloud.sketchy.monitoring.Prometheus


object Exceptions {

  def report(e: Throwable) {
    val exceptionType = e.toString.split(':').head
     counter
       .labels(exceptionType)
       .inc()
  }

  private val counter = Prometheus.counter("exceptions",
                                           "exceptions counter",
                                           List("exception"))
}
