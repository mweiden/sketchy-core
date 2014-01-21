package com.soundcloud.sketchy.monitoring

import System.{ currentTimeMillis => now, getProperty => property }
import scala.collection.mutable

import io.prometheus.client.metrics.{ Counter, Summary }


object Prometheus {
  private val counters  = mutable.Map[String,Counter]()
  private val summaries = mutable.Map[String,Summary]()

  def counter(
    namespace: String,
    name: String,
    documentation: String,
    labels: List[String]) = {
    if (!counters.contains(name)) {
      counters(name) = Counter.newBuilder()
        .namespace(namespace)
        .name(name)
        .labelNames(labels:_*)
        .documentation(documentation)
        .build()
    }
    counters(name)
  }

  def summary(
    namespace: String,
    name: String,
    documentation: String,
    labels: List[String]) = {
    if (!summaries.contains(name)) {
      summaries(name) = Summary.newBuilder()
        .namespace(namespace)
        .name(name)
        .labelNames(labels:_*)
        .targetQuantile(0.5, 0.05)
        .targetQuantile(0.99, 0.001)
        .documentation(documentation)
        .build()
    }
    summaries(name)
  }
}

/**
 * Simple Scala helper for prometheus setup
 *
 * the main goal here is to keep the naming conventions consistent. The
 * namespace and documentation fields can be overridden if necessary.
 */
trait Instrumented {

  require(property("network.name") != null)

  val metricsNamespace = "sketchy"

  def metricsTypeName: String
  def metricsSubtypeName: Option[String]

  def metricsNetworkName: Option[String] = Some(property("network.name"))

  def metricsGroupName: String =
    List(Some("sketchy"), metricsNetworkName, metricsSubtypeName).flatten.mkString(".")

  val metricsDocumentation = "Counting metrics for Sketchy!"

  private val baseStrings =
    List(
      metricsNetworkName,
      metricsSubtypeName)

  def metricsName = (baseStrings :+ Some("total")).flatten.mkString("_")
  def timerName = (baseStrings :+ Some("timer")).flatten.mkString("_")

  private lazy val prometheusTimer = Prometheus.summary(
    metricsNamespace,
    timerName,
    metricsDocumentation,
    List())

  def prometheusCounter(labels: String*) = Prometheus.counter(
    metricsNamespace,
    metricsName,
    metricsDocumentation,
    labels.toList)

  def prometheusSummary(labels: String*) = Prometheus.summary(
    metricsNamespace,
    metricsName,
    metricsDocumentation,
    labels.toList)

  // Must use time as a control statment as in time { func }
  def timer[T](func: => T): T = {
    val tic = now
    val result = func
    val toc = now - tic
    prometheusTimer.newPartial()
      .labelPair(metricsSubtypeName.getOrElse("type"), metricsTypeName)
      .apply()
      .observe(toc)
    result
  }
}
