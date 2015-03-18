package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.events.{Event, UserEvent}
import org.slf4j.{LoggerFactory,Logger}

/**
 * Logs events to stdout.
 */
class LoggingAgent(tag: String, serialize: Event => String) extends Agent {

  def on(event: Event): Seq[Event] = {
    val suffix = event match {
      case x: UserEvent => x.action.toString.toLowerCase
      case _ => "create"
    }

    val message = "%s.%s/JSON %s".format(event.getName, suffix, serialize(event))
    logger.info(message)
    Nil
  }
}
