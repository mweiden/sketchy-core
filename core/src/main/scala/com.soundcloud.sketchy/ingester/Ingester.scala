package com.soundcloud.sketchy.ingester

import java.util.{ Date, Timer, TimerTask }

import com.soundcloud.sketchy.broker.{ HaBroker, HaBrokerEnvelope }
import com.soundcloud.sketchy.monitoring.Prometheus
import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.{ Tick, Event }

import org.scalatra._

object Ingester {
  val counter = Prometheus.counter("ingester",
                                   "ingester counts",
                                   List("ingester", "kind", "status"))
  val timer = Prometheus.timer("ingester",
                               "ingester timer",
                               List("ingester", "kind"))
}

abstract trait Ingester extends Notifying {
  import Ingester._

  def metricsNameArray   = this.getClass.getName.split('.')
  val metricsName = metricsNameArray(metricsNameArray.length - 2).toLowerCase
  val metricsSubtypeName = metricsNameArray(metricsNameArray.length - 1)

  def kind: String

  override def emit(event: Option[Event]) = {
    timer.time(metricsSubtypeName, kind) {
      super.emit(event)
    }

    counter
      .labels(metricsSubtypeName,
              kind,
              if (event.isDefined) "success" else "failure")
      .inc()
  }

  def enable()

}


abstract class HTTPIngester
  extends ScalatraServlet with Ingester with Notifying {

  def kind: String

  override def emit(event: Option[Event]) = {
    Ingester.timer.time("HTTPIngester", kind) {
      super.emit(event)
    }

    Ingester.counter
      .labels(metricsSubtypeName,
              kind,
              if (event.isDefined) "success" else "failure")
      .inc()
  }

  // You must define some HTTP endpoints and parse using parsing
  // e.g. POST /:network/:useraction.action/:event

  def enable() {
    HTTPIngester.register(this)
  }
}

object HTTPIngester {
  private val servletRegistry = scala.collection.mutable.Queue[HTTPIngester]()

  def register(ingester: HTTPIngester) {
    if (!servletRegistry.contains(ingester)) servletRegistry += ingester
  }

  def servlets = List(servletRegistry:_*)
}


/**
 * Connectors
 */
abstract class HaBrokerIngester[T <: Event](
  broker: HaBroker,
  queue: String,
  exchange: String,
  key: String,
  autoDelete: Boolean = true) extends Ingester {

  def event(json: String): Option[T]

  def kind = key

  def enable() {
    val consumer = broker.consumer

    consumer.subscribe(queue, exchange, key, (envelope: HaBrokerEnvelope) => {
      val json: String = new String(envelope.payload)
      emit(event(json))
      envelope.ack()
    }, autoDelete)
  }
}

/**
 * Produces a tick (default: every hour)
 */
class TimerIngester(
  delay: Long = 60 * 1000,
  interval: Long = 60 * 60 * 1000) extends Ingester {

  var lastTick = new Date()

  def kind = "timer"

  def event() = {
    val now = new Date()
    val tick = Tick(lastTick)
    lastTick = now
    Some(tick)
  }

  def enable() = {
    val timer = new Timer()
    val task = new TimerTask() {
      def run() = emit(event())
    }
    timer.schedule(task, delay, interval)
  }
}

