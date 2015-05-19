package com.soundcloud.sketchy.monitoring

import System.{currentTimeMillis => now, getProperty => property}

import io.prometheus.client.{Gauge, Counter, Histogram}


object Prometheus {

  require(property("process.name") != null)
  require(property("metrics.namespace") != null)

  private def clean(s: String) = s.replaceAll("[^a-zA-Z]", "")

  lazy val projectName: String = clean(property("metrics.namespace"))

  lazy val processName: String = clean(property("process.name"))

  val namespace = s"${projectName}_${processName}"

  val defaultBuckets = List(.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10)

  def counter(
    name: String,
    documentation: String,
    labels: List[String]): Counter =
      Counter.build()
        .namespace(namespace)
        .name(s"${name}_total")
        .labelNames(labels:_*)
        .help(documentation)
        .register()

  def histogram(
    name: String,
    documentation: String,
    labels: List[String],
    buckets: List[Double] = defaultBuckets): Histogram =
      Histogram.build()
        .namespace(namespace)
        .name(name)
        .buckets(buckets:_*)
        .labelNames(labels:_*)
        .help(documentation)
        .register()

  def gauge(
    name: String,
    documentation: String,
    labels: List[String]): Histogram =
      Gauge.build()
        .namespace(namespace)
        .name(name)
        .labelNames(labels:_*)
        .help(documentation)
        .register()


  def timer(
    name: String,
    documentation: String,
    labels: List[String]): Timer = Timer(
      Histogram.build()
        .namespace(namespace)
        .name(s"${name}_timer")
        .labelNames(labels:_*)
        .help(documentation)
        .register())

  case class Timer(h: Histogram) {
    // Must use time as a control statment as in time { func }
    def time[T](labels: String*)(func: => T): T = {
      val tic = now
      val result = func
      val toc = now - tic
      h.labels(labels:_*).observe(toc)
      result
    }
  }
}
