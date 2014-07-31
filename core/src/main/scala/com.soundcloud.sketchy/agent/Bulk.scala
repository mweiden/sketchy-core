package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.Tokenizer

/**
 * Fingerprinted MessageLike; prepared for comparison using Jaccard
 * coefficient.
 */
class BulkStatisticsAgent(
  statsContext: Context[BulkStatistics],
  tokenizer: Tokenizer) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case userEvent: UserEvent if userEvent.noSpamCheck => Nil
      case tpl: MessageLike => update(tpl)
      case _ => Nil
    }
  }

  protected def update(message: MessageLike): Seq[Event] = {
    message.senderId match {
      case Some(id) => {
        val stats: List[Int] = tokenizer.fingerprint(message.content).sorted
        /*
         * It's possible for MessageLike to contain no fingerprints. This
         * will happen for the comment "!!", since ! is discarded.
         */
        if (!stats.isEmpty) {
          statsContext.append(id, BulkStatistics(message.key, stats))
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
 * Bulk detection. Comparison made using Jaccard coefficient between messages
 * (A-B / A+B)
 */
class BulkDetectorAgent(
  statsContext: Context[BulkStatistics],
  minSpams: Int = 5,
  maxDist: Double = 0.6) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => detect(userIds)
      case _ => Nil
    }
  }

  protected def detect(userIds: List[Long]): Seq[Event] = {
    val now = new Date
    val bulk = statsContext.getPartitioned(userIds.map(_.toLong))
    bulk.foldLeft(List[SketchySignal]())((s, x) => {
      val (strength, userKeys) = findBulk(x._2.slice(0, 1000))
      val ids = userKeys.map(_._2.id).toList

      userKeys.groupBy(_._1).foldLeft(s)((sig, x) => {
        val (userId, keys) = (x._1.toInt, x._2.map(_._2))
        statsContext.delete(userId, keys)
        sig :+ SketchySignal(userId, keys.head.kind, ids, "Bulk", strength, now)
      })
    })
  }

  private def findBulk(
    content: Seq[(Long, BulkStatistics)]
    ): (Double, Seq[(Long, UserEventKey)]) = {

    val empty: (Double, Seq[(Long, UserEventKey)]) = (0.0, Seq())

    if (content.length < minSpams) return empty

    val stats = content.map(_._2)
    val s = stats.max(new Ordering[BulkStatistics] {
      def compare(x: BulkStatistics, y: BulkStatistics): Int =
        similar(x, stats).length compare similar(y, stats).length
    })

    val (distance, userKeys) = content.foldLeft(empty)((x, y) => {
      val d = dist(s, y._2)
      if (d < maxDist) (x._1 + d, x._2 :+ (y._1, y._2.key)) else x
    })
    if (userKeys.length < minSpams) return empty

    val strength = 1.0 - math.pow(distance / userKeys.length, 2.0)
    (strength, userKeys)
  }

  private def similar(x: BulkStatistics, window: Seq[BulkStatistics]) =
    window.filter(y => dist(x, y) < maxDist)

  private def dist(x: BulkStatistics, y: BulkStatistics): Double = {
    var ix = 0
    var iy = 0
    var counter = 0

    while (ix < x.fingerprints.length && iy < y.fingerprints.length) {
      if (x.fingerprints(ix) == y.fingerprints(iy)) counter += 1
      if (x.fingerprints(ix) <= y.fingerprints(iy)) ix += 1 else iy += 1
    }

    1.0 - counter / (3.0 + x.fingerprints.length + y.fingerprints.length - counter)
  }

}

