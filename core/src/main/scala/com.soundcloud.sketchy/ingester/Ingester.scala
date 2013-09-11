package com.soundcloud.sketchy.ingester

import java.util.{ Date, Timer, TimerTask }

import com.soundcloud.sketchy.broker.{ HaBroker, HaBrokerEnvelope }
import com.soundcloud.sketchy.monitoring.Instrumented
import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.{ Tick, Event }

import org.scalatra._

abstract class Ingester extends Notifying with Instrumented {

  override def emit(event: Event) = {
    timer {
      super.emit(event)
    }

    counter.newPartial()
      .labelPair("direction", "outgoing")
      .apply().increment()
  }

  def enable()

  private val counter = prometheusCounter("direction")
}


abstract class HTTPIngester
  extends ScalatraServlet with Notifying with Instrumented {

  override def emit(event: Event) = {
    timer {
      super.emit(event)
    }

    counter.newPartial()
      .labelPair("direction", "outgoing")
      .apply().increment()
  }

  // You must define some HTTP endpoints and parse using parsing
  // e.g. POST /:network/:useraction.action/:event

  def enable() {
    HTTPIngester.register(this)
  }

  private val counter = prometheusCounter("direction")
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

  def event(json: String): T

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

  def event() = {
    val now = new Date()
    val tick = Tick(lastTick)
    lastTick = now
    tick
  }

  def enable() = {
    val timer = new Timer()
    val task = new TimerTask() {
      def run() = emit(event())
    }
    timer.schedule(task, delay, interval)
  }
}
