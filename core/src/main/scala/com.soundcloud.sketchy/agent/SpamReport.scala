package com.soundcloud.sketchy.agent

import java.util.Date

import com.soundcloud.sketchy.context.Context
import com.soundcloud.sketchy.events._

import com.soundcloud.sketchy.context._

/**
 * Write user Report statistics into context
 */
class SpamReportStatisticsAgent(
  statsContext: Context[SpamReportStatistics]) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case report: SpamReport => update(report)
      case _ => Nil
    }
  }

  protected def update(report: SpamReport): Seq[Event] = {
    val lastSignaledAt = report.lastSignaledAt.getOrElse(new Date(0))
    if (lastSignaledAt.before(report.spamPublishedAt)) {
      val stat = SpamReportStatistics(report.key, report.reporterId,
        report.originType, report.spamPublishedAt, lastSignaledAt)

      statsContext.append(report.spammerId, stat)
      UserAction(List(report.spammerId)) :: Nil
    } else {
      Nil
    }
  }
}

/**
 * Detects user Reports
 *
 * If we see reports from minReporters different users OR minSpams reports
 * in total a sketchy signal is emitted
 */
class SpamReportDetectorAgent(
  statsContext: Context[SpamReportStatistics],
  minReporters: Int = 3,
  minSpams: Int = 10) extends Agent {

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => userIds.map(detect _).flatten
      case _ => Nil
    }
  }

  protected def detect(userId: Long): Seq[Event] = {
    val reports = statsContext.get(userId).groupBy(_.originType)
    reports.foldLeft(List[SketchySignal]())((s, x) => {
      /*
       * find and remove outdated reports from memcache
       */
      val lastSignaledAt = x._2.maxBy(_.lastSignaledAt).lastSignaledAt
      val (items, outdated) = x._2.partition(_.originCreatedAt.after(lastSignaledAt))
      statsContext.delete(userId, outdated.map(_.key))

      val uniqueItems = items.map(_.reporterId).toSet
      if (items.size < minSpams && uniqueItems.size < minReporters) s else {
        val keys = items.map(_.key)
        val kind = items.map(_.originType).head
        val ids = keys.map(_.id).toList

        statsContext.delete(userId, keys)
        SketchySignal(userId, kind, ids, "SpamReport", 1.0, new Date()) :: s
      }
    })
  }
}
