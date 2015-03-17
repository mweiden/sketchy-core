package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.events.{Event, UserEvent}
import org.apache.log4j.Logger

/**
 * Logs events to stdout.
 */
class LoggingAgent(
  tag: String,
  serialize: Event => String,
  level: Symbol = 'INFO) extends Agent {

  override val loggerName = this.getClass.getName
  override lazy val logger = Logger.getLogger(loggerName)

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
