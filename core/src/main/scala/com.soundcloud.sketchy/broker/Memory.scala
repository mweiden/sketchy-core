package com.soundcloud.sketchy.broker

import scala.collection.mutable

/**
 * MemoryBroker. Queues can only have one consumer.
 */
class MemoryBroker extends HaBroker {
  val client = new Client()

  def producer(): HaBrokerProducer = new HaBrokerProducer {
    def publish(exchange: String, key: String, payload: String) {
      client.publish(exchange, key, payload)
    }
  }

  def consumer(): HaBrokerConsumer = new HaBrokerConsumer {
    def subscribe(
      queue: String,
      exchange: String,
      key: String,
      callback: (HaBrokerEnvelope) => Unit,
      autoDelete: Boolean = true) {
        client.bind(queue, exchange, key)
        client.consume(queue, callback)
      }
  }

  def produce(route: String, payload: String) {
    val Array(exchange, key) = route.split("#")
    producer.publish(exchange, key, payload)
  }

  /**
   * In memory bindings, queues and consumers.
   */
  class Client {
    val bindings = mutable.Map[String, mutable.ListBuffer[String]]()
    val queues = mutable.Map[String, mutable.Stack[String]]()
    val consumers = mutable.Map[String, (MemoryEnvelope) => Unit]()

    def publish(exchange: String, key: String, payload: String) {
      var bound = bindings.getOrElse(route(exchange, key), Nil)
      bound.flatMap(queues.get(_)).foreach { q => q.push(payload) }
      drain()
    }

    def bind(queue: String, exchange: String, key: String) {
      synchronized {
        val bs = getBindings(route(exchange, key))
        bindings(route(exchange, key)) = bs :+ queue
        queues(queue) = mutable.Stack()
      }
    }

    def consume(queue: String, callback: (HaBrokerEnvelope) => Unit) {
      consumers(queue) = callback
    }

    private def route(exchange: String, key: String): String = {
      exchange + "#" + key
    }

    private def getBindings(route: String): mutable.ListBuffer[String] = {
      if (bindings.contains(route)) bindings(route) else mutable.ListBuffer()
    }

    private def drain() {
      for ((name, q) <- queues) {
        if (consumers.contains(name)) {
          while (!q.isEmpty) {
            consumers(name)(MemoryEnvelope(q.pop()))
          }
        }
      }
    }
  }
}

case class MemoryEnvelope(payload: String) extends HaBrokerEnvelope

