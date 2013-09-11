package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.context.Context

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.Classifier

/**
 * Write Junk statistics into context
 */
class JunkStatisticsAgent(
  statsContext: Context[JunkStatistics],
  classifier: Classifier) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case tpl: MessageLike if(!tpl.noSpamCheck) => update(tpl)
      case _ => Nil
    }
  }

  protected def update(message: MessageLike): Seq[Event] = {
    message.senderId match {
      case Some(id) if message.content.length >= 16 => {
        val stats: Double = classifier.predict(message.content)

        statsContext.append(id, JunkStatistics(message.key, stats))
        UserAction(List(id)) :: Nil
      }
      case _ => Nil
    }
  }
}

/**
 * Detects Junk
 */
class JunkDetectorAgent(
  statsContext: Context[JunkStatistics],
  minSpams: Int = 3,
  confidence: Double = 0.7) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => userIds.map(detect _).flatten
      case _ => Nil
    }
  }

  protected def detect(userId: Int): Seq[Event] = {
    val junk = statsContext.getPartitioned(userId)
    junk.foldLeft(List[SketchySignal]())((s, x) => {
      val items = x._2.filter(_.spamProbability >= confidence)
      if (items.length < minSpams) s else {
        val spamminess = items.foldLeft(0.0)((x, y) => x + y.spamProbability)
        val strength = spamminess / items.length.toDouble
        val keys = items.map(_.key)
        val kind = keys.head.kind
        val ids = keys.map(_.id).toList

        statsContext.delete(userId, keys)
        SketchySignal(userId, kind, ids, "Junk", strength, new Date()) :: s
      }
    })
  }
}
