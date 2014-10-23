package com.soundcloud.sketchy.broker

import com.rabbitmq.client.{ ConnectionFactory,
                             Connection,
                             Channel,
                             DefaultConsumer,
                             Envelope,
                             ShutdownListener,
                             ShutdownSignalException }

import com.rabbitmq.client.AMQP
import scala.collection.JavaConverters._

import net.jodah.lyra._
import net.jodah.lyra.config._
import net.jodah.lyra.util._

/**
 * Ha Rabbit Broker
 */
class HaRabbitBroker(
  publishUri: String,
  consumeUris: List[String]) extends HaBroker {
  import HaRabbitBroker._

  require(valid(publishUri), "need user, pw, host, port: " + publishUri)

  for(uri <- consumeUris) {
    require(valid(uri), "need user, pw, host, port: " + uri)
  }

  def producer(): HaBrokerProducer = new HaBrokerProducer {
    val client = new Client(List(publishUri))

    def publish(exchange: String, key: String, payload: String) {
      client.declareExchange(exchange)
      client.publish(exchange, key, payload)
    }
  }

  def consumer(): HaBrokerConsumer = new HaBrokerConsumer {
    val client = new Client(consumeUris)

    def subscribe(
      queue: String,
      exchange: String,
      key: String,
      callback: (HaBrokerEnvelope) => Unit,
      autoDelete: Boolean = false) {
        client.declareExchange(exchange)
        client.declareQueue(queue, autoDelete)
        client.declareBinding(queue, exchange, key)
        client.consume(queue, callback)
      }
  }

  private class Client(uris: List[String]) {

    private val decomposedUris = uris.map(uri => {
      val UriFormat(user, pass, host, port) = uri;
      (user, pass, host + ":" + port)
    })

    private val addresses = decomposedUris.map(_._3)
    private val passwords = decomposedUris.map(_._2)
    private val usernames = decomposedUris.map(_._1)

    require(passwords.distinct.length == 1)
    require(usernames.distinct.length == 1)

    val config = new Config()
       .withRecoveryPolicy(RecoveryPolicies.recoverAlways())
       .withRetryPolicy(new RetryPolicy()
           .withBackoff(Duration.seconds(1), Duration.seconds(10))
           .withMaxDuration(Duration.minutes(1)));

    private val options     = addresses.map(address => {
      new ConnectionOptions().withAddresses(address)
        .withUsername(usernames.head)
        .withPassword(passwords.head)})

    private val connections = options.map(opt => Connections.create(opt, config))
    private val channels    = connections.map(_.createChannel())
    channels.map(_.basicQos(1))

    def declareExchange(name: String) =
      channels.map(_.exchangeDeclare(name, "direct", true))

    def declareQueue(name: String, autoDelete: Boolean) =
      channels.map(_.queueDeclare(name, false, false, autoDelete, null))

    def declareBinding(queue: String, exchange: String, key: String) =
      channels.map(_.queueBind(queue, exchange, key))

    def publish(exchange: String, key: String, payload: String) =
      channels.map(_.basicPublish(exchange, key, null, payload.getBytes()))

    def consume(queue: String, callback: (HaBrokerEnvelope) => Unit) =
      channels.map(chan => {
        chan.basicConsume(queue, false, new RabbitConsumer(chan, callback))
      })

    // rabbitmq api artifact
    private class RabbitConsumer(
      channel: Channel,
      callback: (HaBrokerEnvelope) => Unit) extends DefaultConsumer(channel) {
      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: Array[Byte]) {

        val haEnvelope = new HaBrokerEnvelope {
          override def ack() = channel.basicAck(envelope.getDeliveryTag(), false)
          def payload = new String(body)
        }

        callback(haEnvelope)
      }
    }

  }
}

object HaRabbitBroker {
  val UriFormat = """^amqp://([^:]*):([^@]*)@([^:]*):(.*)$""".r

  def valid(uri: String): Boolean = {
    try { val UriFormat(user, pass, host, port) = uri; true } catch {
      case e: Throwable => false
    }
  }
}

