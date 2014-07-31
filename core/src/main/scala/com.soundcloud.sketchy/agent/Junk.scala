package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.context.Context

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.Classifier

import scala.collection.immutable.HashSet


object JunkConfig {

  case class ClassConfig(
    label: Int,
    confidence: Double,
    limit: Int,
    name: String)

  val standardConfig = List(
    ClassConfig(1, 0.7, 3, "Spam")
  )

  val junkClasses = HashSet(standardConfig.map(_.label).toList:_*)

  def mapify(cfgs: ClassConfig*) = {
    cfgs.map(c => c.label -> c).toMap
  }
}

/**
 * Write Junk statistics into context
 */
class JunkStatisticsAgent(
  statsContext: Context[JunkStatistics],
  classifier: Classifier,
  junkClasses: HashSet[Int] = JunkConfig.junkClasses) extends Agent {

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




/**
 * Detects Junk
 */
class JunkDetectorAgent(
  statsContext: Context[JunkStatistics],
  classes: List[JunkConfig.ClassConfig] = JunkConfig.standardConfig) extends Agent {

  import JunkConfig._

  val cfg = mapify(classes:_*)

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => userIds.map(detect _).flatten
      case _ => Nil
    }
  }

  protected def detect(userId: Long): Seq[Event] = {
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
            List("Junk", cfg(label).name).mkString("_"),
            avgClassProb,
            new Date()))
        }
      }}.flatten.toList
    }}.flatten.toList
  }
}
