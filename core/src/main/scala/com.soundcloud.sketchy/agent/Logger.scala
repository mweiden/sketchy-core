package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.events.{ Event, UserEvent }
import com.soundcloud.sketchy.util.{ Formatting, Logging }


/**
 * Logs events to stdout.
 */
class LoggingAgent extends Agent with Logging {
  def on(event: Event): Seq[Event] = {
    val suffix = event match {
      case x: UserEvent if x.wasDeleted => "delete"
      case _ => "create"
    }
    val message = "%s.%s/JSON %s".format(event.getName, suffix, event.json)
    log.log('INFO, message, System.out, meta(event)); Nil
  }

  protected def meta(event: Event) = {
    List("x-category" -> Formatting.scored(event.getName))
  }
}
