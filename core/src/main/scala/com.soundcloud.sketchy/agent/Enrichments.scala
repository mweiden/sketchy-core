package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.access.SketchyAccess
import com.soundcloud.sketchy.util.Logging

import com.soundcloud.sketchy.events.{ Event, MessageLike }


/**
 * Enriches message like objects with an extra field
 */
class MessageLikeEnrichAgent(sketchy: SketchyAccess) extends Agent with Logging {

  def on(event: Event): Seq[Event] = {
    val broadcast = event match {
      case text: MessageLike if (text.senderId.isDefined) =>
        text :: Nil
      case _ => Nil
    }
    broadcast.map(enrich _)
  }

  private def enrich(message: MessageLike): MessageLike = {
    val sender = message.senderId.get

    message.trusted = Some(sketchy.trusted(sender))
    message
  }
}

