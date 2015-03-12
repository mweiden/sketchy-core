package com.soundcloud.example.agent

import java.util.Date

import com.soundcloud.sketchy.util.Logging
import com.soundcloud.sketchy.agent.Agent

import com.soundcloud.sketchy.util.HttpClient

import com.soundcloud.example.events.User
import com.soundcloud.sketchy.events.{
  Event,
  SketchySignal
}


/**
 * Example use of HTTP library
 *
 * Disclaimer: not tested in production at SoundCloud
 */
class BlacklistAgent extends Agent {

  val http = new HttpClient("BlacklistRequesterAgent")

  def on(tuple: Event): Seq[Event] = {
    tuple match {
      case user: User if user.ips.isDefined && user.id.isDefined => check(user)
      case _ => Nil
    }
  }


  private def check(user: User): Seq[SketchySignal] = {
    val responses = user.ips.get.map(ip =>
      http.post("http://www.stopforumspam.com/api?ip=%s".format(ip), "", false))

    responses.foreach(response =>
      if (response._1 == 200) {
        meter("post", "success")
      } else {
        meter("post", "failure")
      }
    )

    if (responses.map(response => response._2.contains("<appears>yes</appears>"))
      .foldLeft(false)(_ || _)) {
      new SketchySignal(
        userId = user.id.get,
        kind = user.kind,
        items = List(),
        detector = "Blacklist",
        strength = 1.0,
        createdAt = new Date()) :: Nil
    } else {
      Nil
    }
  }

  private val counter = prometheusCounter("request", "status")
  private def meter(request: String, status: String) {
    counter.newPartial()
      .labelPair("request", request)
      .labelPair("status", status)
      .apply().increment()
  }
}

