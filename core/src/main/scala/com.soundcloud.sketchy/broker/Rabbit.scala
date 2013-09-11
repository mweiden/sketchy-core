package com.soundcloud.sketchy.broker

/**
 * HA RabbitMQ broker
 */
class HaRabbitBroker(
  publishUri: String,
  consumeUris: List[String]) extends HaBroker {
  import HaRabbitBroker.valid

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
    import com.rabbitmq.client._
    import net.joshdevins.rabbitmq.client.ha._

    private val factories = uris.map(new SketchyConnectionFactory(_))
    private val connections = factories.map(f => f.newConnection(Array(f.address)))
    private val channels = connections.map(_.createChannel())
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

    // RabbitMQ API artifact
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

    // HA RabbitMQ API artifact
    private class SketchyConnectionFactory(uri: String) extends HaConnectionFactory {
      val HaRabbitBroker.UriFormat(username, password, hostname, port) = uri
      val address = new Address(hostname, port.toInt)

      setUri(uri)
    }
  }
}

object HaRabbitBroker {
  val UriFormat = """^amqp://([^:]*):([^@]*)@([^:]*):(.*)$""".r

  def valid(uri: String): Boolean = {
    try { val UriFormat(user, pass, host, port) = uri; true } catch {
      case _ => false
    }
  }
}

