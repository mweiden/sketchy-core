package com.soundcloud.example.agent

import com.soundcloud.sketchy.agent.BulkStatisticsAgent
import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events.{ Event, MessageLike }
import com.soundcloud.sketchy.util.Tokenizer

import com.soundcloud.example.util.SimpleTokenizer
import com.soundcloud.example.events.{ Comment, Message }

/**
 * Fingerprinted MessageLike; prepared for comparison using Jaccard
 * coefficient.
 */
class ExampleBulkStatisticsAgent(
  context: Context[BulkStatistics],
  tokenizer: Tokenizer = new SimpleTokenizer)
  extends BulkStatisticsAgent(context, tokenizer) {

  override def on(event: Event): Seq[Event] = {
    event match {
      case c: Comment => if(!c.toMyself) super.update(c) else Nil
      case m: Message => if(!m.adminMessage) super.update(m) else Nil
      case m: MessageLike => super.update(m)
      case _ => Nil
    }
  }
}

