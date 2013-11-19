package com.soundcloud.sketchy.access

import java.sql.ResultSet
import java.util.Date

import java.text.SimpleDateFormat

import scala.slick.lifted.Query
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.dynamicSession
import scala.slick.ast.TypeMapping

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

  private val sketchyItems = TableQuery[SketchyItems]
  private val sketchyScores = TableQuery[SketchyScores]
  private val trustedUsers = TableQuery[TrustedUsers]

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
      (for { score <- sketchyScores
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
    db.withFailover("insertItem", true, isQuiet = true) {
      sketchyItems.insert(SketchyItem(id, kind, createdAt))
    }.getOrElse(0) > 0

  private def updateItem(id: Int, kind: String, newCreatedAt: Date): Boolean =
    db.withFailover("updateItem", true) {
      val q = for { item <- sketchyItems if item.id === id && item.kind === kind }
        yield item.createdAt
      q.update(newCreatedAt)
    }.getOrElse(0) > 0

  // TODO: INSERT IGNORE, when available
  private def insertScore(scores: SketchyScore*): Boolean =
    db.withFailover("insertScore", true, isQuiet = true) {
      sketchyScores.insertAll(scores:_*)
    }.isDefined


  private class SketchyItems(tag: Tag) extends Table[SketchyItem](tag, "sketchy_items") {
    def id = column[Int]("id", O.PrimaryKey)
    def kind = column[String]("kind", O.PrimaryKey)
    def createdAt = column[Date]("created_at")
    def * = (id, kind, createdAt) <> (SketchyItem.tupled, SketchyItem.unapply)
  }

  private class SketchyScores(tag: Tag) extends Table[SketchyScore](tag, "sketchy_scores") {
    def user_id = column[Int]("user_id")
    def kind = column[String]("kind")
    def signals = column[Int]("signals")
    def state = column[Int]("state")
    def score = column[Double]("score")
    def probability = column[Double]("probability")
    def lastSignaledAt = column[Date]("last_signaled_at")
    def createdAt = column[Date]("created_at")
    def * = (
      user_id,
      kind,
      signals,
      state,
      score,
      probability,
      lastSignaledAt,
      createdAt) <> (SketchyScore.tupled, SketchyScore.unapply)
  }

  private class TrustedUsers(tag: Tag) extends Table[TrustedUser](tag, "trusted_users") {
    def user_id = column[Int]("user_id", O.PrimaryKey) // This is the primary key column
    def reason = column[String]("reason")
    def created_at = column[Date]("created_at")
    def * = (user_id, reason, created_at) <> (TrustedUser.tupled, TrustedUser.unapply)
  }

  implicit val DateMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getTime),
      t => new java.util.Date(t.getTime))
}
