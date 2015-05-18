package com.soundcloud.sketchy.ingester

import java.util.{ Date, Timer, TimerTask }

import com.soundcloud.sketchy.broker.{ HaBroker, HaBrokerEnvelope }
import com.soundcloud.sketchy.monitoring.Instrumented
import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.{ Tick, Event }

import org.scalatra._

abstract trait Ingester extends Notifying with Instrumented {

  def metricsNameArray   = this.getClass.getName.split('.')
  def metricsTypeName    = metricsNameArray(metricsNameArray.length - 1)
  def metricsSubtypeName = Some(metricsNameArray(metricsNameArray.length - 2))

  def kind: String

  override def emit(event: Option[Event]) = {
    timer {
      super.emit(event)
    }

    counter
      .labels("outgoing", metricsTypeName, kind, if (event.isDefined) "success" else "failure")
      .inc()
  }

  def enable()

  protected val counter = prometheusCounter("ingester", List("direction", "ingester", "kind", "status"))
}


abstract class HTTPIngester
  extends ScalatraServlet with Ingester with Notifying with Instrumented {

  def kind: String

  override def emit(event: Option[Event]) = {
    timer {
      super.emit(event)
    }

    counter
      .labels("outgoing", metricsTypeName, kind, if (event.isDefined) "success" else "failure")
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

