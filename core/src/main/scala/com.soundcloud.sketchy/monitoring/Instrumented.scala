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

  def counter(
    name: String,
    documentation: String,
    labels: List[String]): Counter =
      Counter.build()
        .namespace(namespace)
        .name(name)
        .labelNames(labels:_*)
        .help(documentation)
        .register()

  def histogram(
    name: String,
    documentation: String,
    labels: List[String]): Histogram =
      Histogram.build()
        .namespace(namespace)
        .name(name)
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
}



/**
 * Simple Scala helper for prometheus setup
 *
 * the main goal here is to keep the naming conventions consistent. The
 * namespace and documentation fields can be overridden if necessary.
 */
abstract trait Instrumented {

  import Prometheus._

  val metricsName: String
  val metricsSubtypeName: Option[String] = None

  val metricsDocumentation = "Counting metrics for Sketchy!"

  private lazy val prometheusTimer = Prometheus.histogram(
    s"${metricsName}_timer",
    s"Timer for $metricsName operations!",
    List("type"))

  // Must use time as a control statment as in time { func }
  def timer[T](func: => T): T = {
    val tic = now
    val result = func
    val toc = now - tic
    prometheusTimer
      .labels(metricsSubtypeName.getOrElse("singleton"))
      .observe(toc)
    result
  }
}
