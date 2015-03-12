package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.events.{ JSON, Event, UserEvent }
import com.soundcloud.sketchy.util.Logging
import org.apache.log4j.Logger
import play.api.libs.json.Writes

/**
 * Logs events to stdout.
 */
class LoggingAgent(
  tag: String,
  serialize: Event => String,
  level: Symbol = 'INFO) extends Agent {

  override val loggerName = this.getClass.getName
  override lazy val logger = Logging.getLogger(loggerName)

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
