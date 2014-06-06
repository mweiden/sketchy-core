package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.context.Context

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.Classifier

import scala.collection.immutable.HashSet

/**
 * Write Junk statistics into context
 */
class JunkStatisticsAgent(
  statsContext: Context[JunkStatistics],
  classifier: Classifier,
  junkClasses: HashSet[Int] = HashSet(JunkDetectorAgent.standardConfig.map(_.label).toList:_*)) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case userEvent: UserEvent if userEvent.noSpamCheck => Nil
      case tpl: MessageLike => update(tpl)
      case _ => Nil
    }
  }

  protected def update(message: MessageLike): Seq[Event] = {
    message.senderId match {
      case Some(id) if message.content.length >= 16 => {
        val (label, probability) = classifier.predict(message.content)

        if (junkClasses.contains(label)) {
          statsContext.append(id, JunkStatistics(message.key, label, probability))
          UserAction(List(id)) :: Nil
        } else {
          Nil
        }
      }
      case _ => Nil
    }
  }
}



object JunkDetectorAgent {

  case class ClassConfig(
    label: Int,
    confidence: Double,
    limit: Int,
    name: String)

  val standardConfig = List(
    ClassConfig(1, 0.7, 3, "Spam")
  )

  def mapify(cfgs: ClassConfig*) = {
    cfgs.map(c => c.label -> c).toMap
  }
}

/**
 * Detects Junk
 */
class JunkDetectorAgent(
  statsContext: Context[JunkStatistics],
  classes: List[JunkDetectorAgent.ClassConfig] = JunkDetectorAgent.standardConfig) extends Agent {

  import JunkDetectorAgent._

  val cfg = mapify(classes:_*)

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => userIds.map(detect _).flatten
      case _ => Nil
    }
  }

  protected def detect(userId: Int): Seq[Event] = {
    val allStats = statsContext.getPartitioned(userId)

    allStats.par.map{case (kind, allClassStats) => {
      allClassStats.par.groupBy(_.label).map{case (label, stats) => {
        val items = stats.filter(_.probability >= cfg(label).confidence).toList

        if (items.length < cfg(label).limit) {
          None
        } else {
          val avgClassProb =
            items.par.map(_.probability).reduce(_ + _) / items.length.toDouble
          val keys = items.map(_.key)
          val kind = keys.head.kind
          val ids = keys.map(_.id).toList

          statsContext.delete(userId, keys)

          Some(SketchySignal(
            userId,
            kind,
            ids,
            "Junk" + ":" + cfg(label).name,
            avgClassProb,
            new Date()))
        }
      }}.flatten.toList
    }}.flatten.toList
  }
}
