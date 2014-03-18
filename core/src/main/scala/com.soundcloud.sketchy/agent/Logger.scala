package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.events.{ Event, UserEvent }
import com.soundcloud.sketchy.util.{ Formatting, Logging }


/**
 * Logs events to stdout.
 */
class LoggingAgent(tag: String, level: Symbol = 'INFO) extends Agent with Logging {

  def on(event: Event): Seq[Event] = {
    val suffix = event match {
      case x: UserEvent => x.action.toString.toLowerCase
      case _ => "create"
    }

    val message = "%s.%s/JSON %s".format(event.getName, suffix, event.json)

    log.log(level, message, System.out, List(("level", tag)))

    Nil
  }

}
