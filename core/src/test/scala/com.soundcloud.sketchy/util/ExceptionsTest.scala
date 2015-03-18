package com.soundcloud.sketchy.util

import org.scalatest.FlatSpec


class ExceptionsTest extends FlatSpec {

  behavior of "the exceptions object metrics"

  it should "have well-formed metric names" in {
    assert(Exceptions.metricsName === "test_exceptions_total")
  }
}
