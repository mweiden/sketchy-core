package com.soundcloud.sketchy.monitoring

import System.{ currentTimeMillis => now }
import scala.collection.mutable

import com.soundcloud.sketchy.util.Naming
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
trait Instrumented extends Naming {

  val metricsNamespace = "sketchy"
  val metricsDocumentation = "Counting metrics for Sketchy!"
  def metricsName = List(networkName, subtypeName, "total").mkString("_")

  private val prometheusTimer = Prometheus.summary(
    metricsNamespace,
    List(networkName, subtypeName, "timer").mkString("_"),
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
      .apply()
      .observe(toc)
    result
  }
}
