package com.soundcloud.example.access

import java.text.SimpleDateFormat
import java.util.Date

import com.soundcloud.example.util.Database
import com.soundcloud.sketchy.access.{TrustedUser, SketchyReputations}
import com.soundcloud.sketchy.events.{SketchyItem, SketchyScore, SketchySignal}

import scala.slick.driver.MySQLDriver.simple.Database.dynamicSession
import scala.slick.driver.MySQLDriver.simple._


/**
 * Reputation in sketchy db
 */
class MySqlSketchyReputations(db: Database) extends SketchyReputations {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  protected val sketchyItems = TableQuery[SketchyItems]
  protected val sketchyScores = TableQuery[SketchyScores]
  protected val trustedUsers = TableQuery[TrustedUsers]

  def select(userId: Long, kind: String): Option[SketchyScore] =
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

  def trusted(userId: Long): Boolean =
    db.withFailover("trusted", false) {
      trustedUsers.filter(user => user.user_id === userId).list
    }.exists(_.nonEmpty)

  def sketchy(kind: String, id: Long): Boolean =
    db.withFailover("sketchy", false) {
      selectItem(kind, id)
    }.getOrElse(None).isDefined

  def selectItem(kind: String, id: Long): Option[SketchyItem] =
    db.withFailover("selectItem", false) {
      sketchyItems.filter(item => item.kind === kind && item.id === id).list
    }.getOrElse(List()).headOption

  /**
   * private helpers
   */
  protected def mergeItems(sig: SketchySignal): Boolean =
    sig.items.map(id =>
      updateItem(id, sig.kind, sig.createdAt) ||
      insertItem(id, sig.kind, sig.createdAt)
    ).forall(_ == true)

  protected def insertItem(id: Long, kind: String, createdAt: Date): Boolean =
    db.withFailover("insertItem", true, isQuiet = true) {
      sketchyItems.insert(SketchyItem(id, kind, createdAt))
    }.getOrElse(0) > 0

  protected def updateItem(id: Long, kind: String, newCreatedAt: Date): Boolean =
    db.withFailover("updateItem", true) {
      val q = for { item <- sketchyItems if item.id === id && item.kind === kind }
        yield item.createdAt
      q.update(newCreatedAt)
    }.getOrElse(0) > 0

  // TODO: INSERT IGNORE, when available
  protected def insertScore(scores: SketchyScore*): Boolean =
    db.withFailover("insertScore", true, isQuiet = false) {
      sketchyScores.insertAll(scores:_*)
    }.isDefined

  def selectTrustedUser(userId: Long): Option[TrustedUser] = {
    db.withFailover("select_trusted_user", false) {
      trustedUsers.filter(user => user.user_id === userId).firstOption
    }.getOrElse(None)
  }

  protected class SketchyItems(tag: Tag) extends Table[SketchyItem](tag, "sketchy_items") {
    def id = column[Long]("id", O.PrimaryKey)
    def kind = column[String]("kind", O.PrimaryKey)
    def createdAt = column[Date]("created_at")
    def * = (id, kind, createdAt) <> (SketchyItem.tupled, SketchyItem.unapply)
  }

  protected class SketchyScores(tag: Tag) extends Table[SketchyScore](tag, "sketchy_scores") {
    def user_id = column[Long]("user_id")
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

  protected class TrustedUsers(tag: Tag) extends Table[TrustedUser](tag, "trusted_users") {
    def user_id = column[Long]("user_id", O.PrimaryKey) // This is the primary key column
    def reason = column[String]("reason")
    def created_at = column[Date]("created_at")
    def * = (user_id, reason, created_at) <> (TrustedUser.tupled, TrustedUser.unapply)
  }

  implicit val DateMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getTime),
      t => new java.util.Date(t.getTime))
}
