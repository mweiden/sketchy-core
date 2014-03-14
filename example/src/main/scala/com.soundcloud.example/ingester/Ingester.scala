package com.soundcloud.example.ingester

import scala.util.Random

import com.soundcloud.sketchy.broker.HaBroker
import com.soundcloud.sketchy.ingester._
import com.soundcloud.sketchy.events.Parsing
import com.soundcloud.sketchy.util.Formatting
import com.soundcloud.sketchy.events.{ Action, UserEvent }

import org.scalatra._


class RabbitUserEventIngester[T <: UserEvent](
  broker: HaBroker,
  network: String,
  userAction: Action, // create, update or destroy
  key: String)(implicit mf: Manifest[T])
  extends HaBrokerIngester[UserEvent](
    broker,
    "sketchy.%s.ingester.%s.%s".format(
      network, Formatting.scored(key),
      userAction.toString.toLowerCase),
    "live.event.%s".format(userAction.toString.toLowerCase),
    key) with Parsing {

  def event(json: String): Option[T] = {
    val event = extractor(json).extract[T]
    event.action = userAction
    Some(event)
  }
}


class HTTPUserEventIngester[T <: UserEvent](
  network: String,
  userAction: Action,
  key: String)(implicit mf: Manifest[T]) extends HTTPIngester with Parsing {

  def kind = key

  val actionName = userAction.toString.toLowerCase

  private def event(json: String): Option[T] = {
    val event = extractor(json).extract[T]
    event.action = userAction
    Some(event)
  }

  post("/%s/%s/%s".format(network, actionName, key)) {
    try {
      emit(event(request.body))
      halt(status = 202, body = "Accepted.")
    } catch {
      case e: java.lang.IllegalArgumentException =>
        halt(status = 400, body = "Malformed input.")
      case e: net.liftweb.json.MappingException =>
        halt(status = 400, body = "Malformed input.")
    }
  }
}


class EvalulateTimerIngester extends TimerIngester(
  delay = (new Random).nextInt(3600000),
  interval = 3600000)

class BatchCheckIngester extends TimerIngester(
  delay = (new Random).nextInt(600000),
  interval = 600000)
