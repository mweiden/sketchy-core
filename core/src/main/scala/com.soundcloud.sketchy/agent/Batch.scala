package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.context.Context
import com.soundcloud.sketchy.events.{
  Event,
  UserAction,
  UserEventKey,
  Tick
}
import com.soundcloud.sketchy.util.Logging

import com.soundcloud.sketchy.context._

/**
 * Fingerprinted MessageLike; prepared for comparison using Jaccard
 * coefficient.
 */
class BatchStatisticsAgent(
  context: Context[BatchStatistics],
  cacheSize: Int = 200) extends Agent with Logging {

  def on(event: Event): Seq[Event] = {
    event match {
      case UserAction(userIds) => update(userIds)
      case tick: Tick => batch()
      case _ => Nil
    }
  }

  protected def update(userIds: List[Int]): Seq[Event] = {
    val activeUsers = context.get.map(_.key.id).toSet
    val numUsers = userIds.foldLeft(activeUsers.size)((num, x) => {
      if (activeUsers.contains(x)) num else {
        context.append(BatchStatistics(UserEventKey("User", x)))
        num + 1
      }
    })

    if (numUsers < cacheSize) Nil else batch()
  }

  private def batch(kind: Option[String] = None): Seq[Event] = {
    val keys = context.get.map(_.key).toList
    context.delete(keys)
    UserAction(keys.map(_.id)) :: Nil
  }
}
