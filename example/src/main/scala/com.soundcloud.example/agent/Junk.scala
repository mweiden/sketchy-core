package com.soundcloud.example.agent

import com.soundcloud.sketchy.agent.JunkStatisticsAgent
import com.soundcloud.sketchy.context.{ Context, JunkStatistics }
import com.soundcloud.sketchy.events.{ Event, MessageLike }
import com.soundcloud.sketchy.util.Classifier

import com.soundcloud.example.events.{ Comment, Message }

/**
 * Write Junk statistics into context
 */
class ExampleJunkStatisticsAgent(
  context: Context[JunkStatistics],
  classifier: Classifier) extends JunkStatisticsAgent(context, classifier) {

  override def on(event: Event): Seq[Event] = {
    event match {
      case c: Comment =>
        if(!c.toMyself && !c.trusted.getOrElse(false)) super.update(c) else Nil
      case m: Message => if(!m.adminMessage) super.update(m) else Nil
      case m: MessageLike => super.update(m)
      case _ => Nil
    }
  }
}

