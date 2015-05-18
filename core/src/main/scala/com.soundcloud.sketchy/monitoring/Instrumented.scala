package com.soundcloud.sketchy.monitoring

import System.{currentTimeMillis => now, getProperty => property}

import scala.collection.mutable

import io.prometheus.client.{Gauge, Counter, Histogram}


object Prometheus {

  def counter(
    namespace: String,
    name: String,
    documentation: String,
    labels: List[String]): Counter =
      Counter.build()
        .namespace(namespace)
        .name(namespace + "_" + name)
        .labelNames(labels:_*)
        .help(documentation)
        .register()

  def histogram(
    namespace: String,
    name: String,
    documentation: String,
    labels: List[String]): Histogram =
      Histogram.build()
        .namespace(namespace)
        .name(namespace + "_" + name)
        .labelNames(labels:_*)
        .help(documentation)
        .register()

  def gauge(
    namespace: String,
    name: String,
    documentation: String,
    labels: List[String]): Histogram =
      Gauge.build()
        .namespace(namespace)
        .name(namespace + "_" + name)
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
trait Instrumented {

  require(property("process.name") != null)
  require(property("metrics.namespace") != null)

  val metricsNamespace = property("metrics.namespace")

  def metricsTypeName: String

  def metricsSubtypeName: Option[String]

  def metricsProcessName: String = property("process.name")

  def metricsGroupName: String = List(
    Some(metricsNamespace),
    Some(metricsProcessName),
    metricsSubtypeName).flatten.mkString(".")

  val metricsDocumentation = "Counting metrics for Sketchy!"

  private val baseStrings = List(
    Some(metricsProcessName),
    metricsSubtypeName)

  def metricsName = (baseStrings :+ Some("total")).flatten.mkString("_")

  def timerName = (baseStrings :+ Some("timer")).flatten.mkString("_")

  private lazy val prometheusTimer = Prometheus.histogram(
    metricsNamespace,
    timerName,
    metricsDocumentation,
    List(metricsSubtypeName.getOrElse("type")))

  def prometheusCounter(labels: String*) = Prometheus.counter(
    metricsNamespace,
    metricsName,
    metricsDocumentation,
    labels.toList)

  def prometheusHistogram(labels: String*) = Prometheus.histogram(
    metricsNamespace,
    metricsName,
    metricsDocumentation,
    labels.toList)


  def prometheusGauge(labels: String*) = Prometheus.gauge(
    metricsNamespace,
    metricsName,
    metricsDocumentation,
    labels.toList)


  // Must use time as a control statment as in time { func }
  def timer[T](func: => T): T = {
    val tic = now
    val result = func
    val toc = now - tic
    prometheusTimer
      .labels(metricsTypeName)
      .observe(toc)
    result
  }
}
