package com.soundcloud.example.ingester

import scala.util.Random

import com.soundcloud.sketchy.broker.HaBroker
import com.soundcloud.sketchy.ingester._
import com.soundcloud.sketchy.events.JSON
import com.soundcloud.sketchy.util.formats.snakify
import com.soundcloud.sketchy.events.{ Action, UserEvent }

import org.scalatra._
import play.api.libs.json.Reads


class RabbitUserEventIngester[T <: UserEvent](
  broker: HaBroker,
  network: String,
  userAction: Action, // create, update or destroy
  key: String)(implicit reader: Reads[T])
  extends HaBrokerIngester[UserEvent](
    broker,
    "sketchy.%s.ingester.%s.%s".format(
      network, snakify(key),
      userAction.toString.toLowerCase),
    "live.event.%s".format(userAction.toString.toLowerCase),
    key) {

  def event(json: String): Option[T] = {
    val event = JSON.fromJson(json.stripLineEnd).get.as[T]
    event.action = userAction
    Some(event)
  }
}


class HTTPUserEventIngester[T <: UserEvent](
  network: String,
  userAction: Action,
  key: String)(implicit reader: Reads[T]) extends HTTPIngester {

  def kind = key

  val actionName = userAction.toString.toLowerCase

  private def event(json: String): Option[T] = {
    val event = JSON.fromJson(json.stripLineEnd).get.as[T]
    event.action = userAction
    Some(event)
  }

  post("/%s/%s/%s".format(network, actionName, key)) {
    try {
      emit(event(request.body))
      halt(status = 202, body = "Accepted.")
    } catch {
      case e: play.api.libs.json.JsResultException =>
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
