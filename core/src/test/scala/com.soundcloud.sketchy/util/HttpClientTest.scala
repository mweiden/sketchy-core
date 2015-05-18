package com.soundcloud.sketchy.util

import org.scalatest.FlatSpec
import com.soundcloud.sketchy.SpecHelper


class HttpClientTest extends FlatSpec {

  val http = new HttpClient("hello")

  behavior of "the http client"

  it should "generate formatted metrics names" in {
    assert(http.metricsName === "hello")
  }
}
