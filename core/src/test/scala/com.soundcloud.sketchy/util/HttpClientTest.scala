package com.soundcloud.sketchy.util

import org.scalatest.FlatSpec
import com.soundcloud.sketchy.SpecHelper


class HttpClientTest extends FlatSpec {

  val http = new HttpClient("hello")

  behavior of "the http client"

  it should "generate formatted metrics names" in {
    assert(http.metricsGroupName === "sketchy.development.http")
    assert(http.metricsName === "development_http_total")
    assert(http.metricsTypeName === "hello")
  }
}
