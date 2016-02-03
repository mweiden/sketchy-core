package com.soundcloud.sketchy.monitoring

import System.{currentTimeMillis => now, getProperty => property}

import io.prometheus.client.{Gauge, Counter, Histogram}
import scala.collection.JavaConversions._


object Prometheus {

  val env = System.getenv.toMap

  lazy val Array(projectName, applicationName) = env.get("APP_NAME").get.split("_")

  val namespace = s"${projectName}_$applicationName"

  val defaultBuckets = List[Double](.01, .05, .1, .5, 1, 2.5, 5, 7.5, 10)
  val timerBuckets: List[Double] = defaultBuckets ++ List[Double](100, 500, 1000, 2000, 5000)

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
    labels: List[String]): Gauge =
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
        .buckets(timerBuckets:_*)
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
