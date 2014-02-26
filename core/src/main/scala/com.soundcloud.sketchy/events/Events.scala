package com.soundcloud.sketchy.events

import java.util.Date
import net.liftweb.json._
import net.liftweb.json.JsonDSL._


/**
 * Itra-actor (agent) message unit
 */
trait Event extends Serializing {
  def id: Option[Int]
  def getName: String = getClass.getName.split('.').last
  def kind: String = getName
}

object Event extends Parsing {
  def fromJson[T <: Event](message: String)(implicit mf: Manifest[T]) = {
    extractor(message).extract[T]
  }
}


/**
 * User action interface
 * Abstract representation of a user's actions on a site
 */
trait Action

case object UserEvent {
  case object Create  extends Action
  case object Destroy extends Action
  case object Update  extends Action
}

trait UserEvent extends Event {
  def key = UserEventKey(kind, id.getOrElse(0))

  var action: Action = UserEvent.Create
  def wasCreated: Boolean = (action == UserEvent.Create)
  def wasDeleted: Boolean = (action == UserEvent.Destroy)
  def wasUpdated: Boolean = (action == UserEvent.Update)

  def recipientId: Option[Int]
  def senderId: Option[Int]
}

trait DeleteOnUpdate {
  val deletedAt: Date
}

trait SpamCheck {
  def noSpamCheck: Boolean
}

/**
 * Something like a private message on the site
 */
trait MessageLike extends Event with SpamCheck {
  def senderId: Option[Int]
  def recipientId: Option[Int]
  def content: String
  def key: UserEventKey
  def toMyself = senderId.isDefined && senderId == recipientId

  // 2-way sender-recipient interaction
  var interaction: Option[Boolean]

  // trusted by policy, e.g. trusted user
  var trusted: Option[Boolean]

  val public: Option[Boolean]
}

/**
 * Relationship between two entities
 *
 * The history of edge changes will be separated by graph id. For example
 * a link from nodes 1 to 7, followed by a link from 7 to 1 will only be
 * counted as a backlink if the graph id is identical.
 */
trait EdgeLike extends Event with SpamCheck {
  val sourceId: Int
  val sinkId: Int
  val edgeKind: String

  val graphId: Option[Int]
  val createdAt: Date
  val isBidirectional: Boolean

  // overlapping with UserEvent
  def senderId: Option[Int]

  def wasCreated: Boolean
}

/**
 * The user(s) did something
 */
case class UserAction(userIds: List[Int]) extends Event {
  val id = None
}

/**
 * Timer
 */
case class Tick(lastTick: Date) extends Event {
  val id = None
}

/**
 * Detected sketchy behavior
 */
case class SketchySignal(
  userId: Int,
  override val kind: String,
  items: List[Int],
  detector: String,
  strength: Double, // signal strength, [0, 1]
  createdAt: Date) extends Event {
  val id = None
}

/**
 * Reported sketchy behavior
 */
case class SpamReport(
  id: Option[Int],
  reporterId: Int,
  spammerId: Int,
  originId: Int,
  originType: String,
  spamPublishedAt: Date,
  lastSignaledAt: Option[Date],
  createdAt: Date,
  updatedAt: Date) extends UserEvent {

  def senderId = Some(reporterId)
  def recipientId = Some(spammerId)
}


/**
 * Sketchy item (e.g. a page)
 */
case class SketchyItem(
  id: Int,
  kind: String,
  createdAt: Date)

/**
 * Scoring of a user
 * An aggregate view of sketchy signals over time
 */
case class SketchyScore(
  userId: Int,
  override val kind: String,
  signals: Int,
  state: Int,
  score: Double,
  probability: Double,
  lastSignaledAt: Date,
  createdAt: Date) extends Event {

  val id = None

  // prior probability of being an abusive user
  val prior: Double = 0.1

  // half-life in days
  val halfLife: Int = 30

  def decayed(date: Date): SketchyScore = {
    val newState = decayedState(date)
    val newScore = decayedScore(date)

    SketchyScore(
      userId,
      kind,
      signals,
      newState,
      newScore,
      prob(newScore),
      lastSignaledAt,
      createdAt)
  }

  def update(signal: SketchySignal): SketchyScore = {
    val newState = if (signals < 2) 0 else decayedState(signal.createdAt) + 1
    val newScore = decayedScore(signal.createdAt) + signal.strength

    SketchyScore(
      userId,
      kind,
      signals + 1,
      newState,
      newScore,
      prob(newScore),
      signal.createdAt,
      createdAt)
  }

  // linearly decayed state
  def decayedState(date: Date): Int =
    scala.math.max(0, state - daysPassed(date).toInt / halfLife)

  // exponentially decayed score
  def decayedScore(date: Date): Double =
    score * scala.math.pow(2.0, -daysPassed(date) / halfLife)

  def prob(score: Double): Double = {
    val posterior = score + prior / (1.0 - prior)
    posterior / (1.0 + posterior)
  }

  def daysPassed(date: Date): Double =
    (date.getTime - lastSignaledAt.getTime) / (24 * 60 * 60 * 1000.0)
}

case class UserEventKey(kind: String, id: Int) {
  def marshalled: String = id.toString + ":" + kind
}

object UserEventKey {
  def unmarshal(key: String): UserEventKey = {
    key.split(':') match {
      case Array(id, kind) => UserEventKey(kind, id.toInt)
      case _ => throw new NullPointerException("UserEventKey parsing error")
    }
  }
}


/**
 * Transformation classes
 * Events that are the result of transformation on user action primitives
 *
 * EdgeChanges encode links, unlinks, relinks and backlinks between objects.
 * EdgeChanges can be applied to EdgeLike objects, and are currently used
 * only as input to the BurstAgent.
 *
 * @param sourceId source id of edge object
 * @param destId sink id of edge object
 * @param actionKind the actormessage kind, e.g. "Affiliation"
 * @param edgeType an EdgeChange.Type
 * @param createdAt the creation time of the event
 */
case class EdgeChange(
  sourceId: Int,
  sinkId: Int,
  ownerId: Option[Int],
  actionKind: String,
  edgeType: EdgeChange.Type,
  createdAt: Date) extends UserEvent {

  // Event level
  def id = None
  override def kind = actionKind

  // UserEvent level
  def senderId = ownerId
  def recipientId = Some(sinkId)
}

case object EdgeChange {
  sealed abstract class Type
  case object Link     extends Type with Action
  case object Relink   extends Type with Action
  case object Unlink   extends Type with Action
  case object Backlink extends Type with Action
}


/**
 * User action primitive classes
 *
 * These include the information encoded in a user action. There are two main
 * types: EdgeLike actions and MessageLike actions.
 */
abstract class AbstractMessageLike extends UserEvent with MessageLike
abstract class AbstractEdgeLike extends UserEvent with EdgeLike


abstract class AbstractAffiliation extends UserEvent with EdgeLike {
  val followeeId: Option[Int]
}

abstract class AbstractComment extends UserEvent with MessageLike {
  val body: Option[String]
}

abstract class AbstractFavoriting extends UserEvent with EdgeLike {
  val itemId: Option[Int]
  val itemKind: Option[String]
}

abstract class AbstractMessage extends UserEvent with MessageLike {
  val subject: Option[String]
  val body: Option[String]
}

abstract class AbstractPost extends UserEvent with MessageLike {
  val title: Option[String]
  val body: Option[String]
}

abstract class AbstractUser extends UserEvent with MessageLike {
  val username: Option[String]
  val permalink: Option[String]
}
