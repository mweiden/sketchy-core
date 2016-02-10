package com.soundcloud.sketchy.access

import java.util.Date

import com.soundcloud.sketchy.events.{SketchyItem, SketchyScore, SketchySignal}


case class TrustedUser(user_id: Long, reason: String, created_at: Date)

/**
 * Reputation in sketchy db
 */
trait AbstractSketchyAccess {
  def select(userId: Long, kind: String): Option[SketchyScore]

  def insert(sig: SketchySignal, items: Boolean = true): Boolean

  def update(sig: SketchySignal, items: Boolean = true): Boolean

  def trusted(userId: Long): Boolean

  def sketchy(kind: String, id: Long): Boolean

  def selectItem(kind: String, id: Long): Option[SketchyItem]
}
