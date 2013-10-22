package com.soundcloud.sketchy.access

import java.sql.ResultSet
import java.util.Date

import java.text.SimpleDateFormat

import scala.slick.lifted.Query
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import scala.slick.lifted.MappedTypeMapper.base
import scala.slick.lifted.TypeMapper

import com.soundcloud.sketchy.events.{
  SketchyItem,
  SketchyScore,
  SketchySignal
}


case class TrustedUser(user_id: Int, reason: String, created_at: Date)

/**
 * Reputation in sketchy db
 */
class SketchyAccess(db: com.soundcloud.sketchy.util.Database) {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private val sketchyItems = Query(SketchyItems)
  private val sketchyScores = Query(SketchyScores)
  private val trustedUsers = Query(TrustedUsers)

  def select(userId: Int, kind: String): Option[SketchyScore] =
    db.withFailover("select", false) {
      val q =
        sketchyScores.filter(score => score.user_id === userId && score.kind === kind)
      q.list
    }.getOrElse(List()).headOption

  def insert(sig: SketchySignal, items: Boolean = true): Boolean = {
    val date = sig.createdAt
    val score = SketchyScore(sig.userId, sig.kind, 0, 0, 0, 0, date, date).update(sig)
    insertScore(score) && (!items || mergeItems(sig))
  }

  def update(sig: SketchySignal, items: Boolean = true): Boolean = {
    val updateStatus = db.withFailover("update", true) {
      var count = 0
      val q = (for { score <- SketchyScores
        if score.user_id === sig.userId && score.kind === sig.kind }
        yield(score))
        .mutate(s => s.row = {count += 1; s.row.update(sig)})
        count
    }.getOrElse(0)
    (updateStatus > 0) && (!items || mergeItems(sig))
  }

  def trusted(userId: Int): Boolean =
    !db.withFailover("trusted", false) {
      trustedUsers.filter(user => user.user_id === userId).list
    }.getOrElse(List()).isEmpty

  def sketchy(kind: String, id: Int): Boolean =
    db.withFailover("sketchy", false) {
      selectItem(kind, id)
    }.getOrElse(None).isDefined

  def selectItem(kind: String, id: Int): Option[SketchyItem] =
    db.withFailover("selectItem", false) {
      sketchyItems.filter(item => item.kind === kind && item.id === id).list
    }.getOrElse(List()).headOption

  /**
   * private helpers
   */
  private def mergeItems(sig: SketchySignal): Boolean = {
    sig.items.map(id =>
      updateItem(id, sig.kind, sig.createdAt) ||
      insertItem(id, sig.kind, sig.createdAt)
    ).foldLeft(true)(_&&_)
  }

  private def insertItem(id: Int, kind: String, createdAt: Date): Boolean =
    db.withFailover("insertItem", true) {
      SketchyItems.insert(SketchyItem(id, kind, createdAt))
    }.getOrElse(0) > 0

  private def updateItem(id: Int, kind: String, newCreatedAt: Date): Boolean =
    db.withFailover("updateItem", true) {
      val q = for { item <- SketchyItems if item.id === id && item.kind === kind }
        yield item.createdAt
      q.update(newCreatedAt)
    }.getOrElse(0) > 0

  private def insertScore(scores: SketchyScore*): Boolean =
    db.withFailover("insertScore", true) {
      SketchyScores.insertAll(scores:_*)
    }.isDefined


  private object SketchyItems extends Table[SketchyItem]("sketchy_items") {
    def id = column[Int]("id", O.PrimaryKey)
    def kind = column[String]("kind", O.PrimaryKey)
    def createdAt = column[Date]("created_at")
    def * = id ~ kind ~ createdAt <> (SketchyItem, SketchyItem.unapply _)
  }

  private object SketchyScores extends Table[SketchyScore]("sketchy_scores") {
    def user_id = column[Int]("user_id")
    def kind = column[String]("kind")
    def signals = column[Int]("signals")
    def state = column[Int]("state")
    def score = column[Double]("score")
    def probability = column[Double]("probability")
    def lastSignaledAt = column[Date]("last_signaled_at")
    def createdAt = column[Date]("created_at")
    def * =
      user_id ~
      kind ~
      signals ~
      state ~
      score ~
      probability ~
      lastSignaledAt ~
      createdAt <> (SketchyScore, SketchyScore.unapply _)
  }

  private object TrustedUsers extends Table[TrustedUser]("trusted_users") {
    def user_id = column[Int]("user_id", O.PrimaryKey) // This is the primary key column
    def reason = column[String]("reason")
    def created_at = column[Date]("created_at")
    def * = user_id ~ reason ~ created_at <> (TrustedUser, TrustedUser.unapply _)
  }

  implicit val DateMapper: TypeMapper[Date] =
    base[java.util.Date, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getTime),
      t => new java.util.Date(t.getTime))
}
