package com.soundcloud.example.events

import java.util.Date
import play.api.libs.json._

import com.soundcloud.sketchy.events._


/**
 * User action primitive classes
 *
 * These include the information encoded in a user action. There are two main
 * types: EdgeLike actions and MessageLike actions.
 */
case class Affiliation(
    id: Option[Long],
    userId: Option[Long],
    followeeId: Option[Long],
    createdAt: Date,
    recommended: Option[Boolean]) extends AbstractAffiliation {

  def senderId = userId
  def recipientId = followeeId

  // edge like
  val sourceId = userId.get
  val sinkId = followeeId.get
  val graphId = None
  val edgeKind = kind
  val isBidirectional = true

  def noSpamCheck = recommended.getOrElse(false)
}

case class Comment(
  id: Option[Long],
  userId: Option[Long],
  body: Option[String],
  itemId: Option[Long],
  itemKind: Option[String],
  itemAuthorId: Option[Long],
  public: Option[Boolean],
  var interaction: Option[Boolean], // enriched field
  var trusted: Option[Boolean], // enriched field
  createdAt: Date) extends AbstractComment {

  def senderId = userId
  def recipientId = itemAuthorId
  def content = body.getOrElse("")

  def noSpamCheck = trusted.getOrElse(false)
}

case class Favoriting(
  id: Option[Long],
  userId: Option[Long],
  itemId: Option[Long],
  itemKind: Option[String],
  createdAt: Date,
  deletedAt: Option[Date]) extends AbstractFavoriting with DeleteOnUpdate {

  def senderId = userId
  def recipientId = itemId

  // edge like
  val sourceId = userId.get
  val sinkId = itemId.get
  val graphId = userId
  val edgeKind = kind
  val isBidirectional = false

  def noSpamCheck = false
}

case class Message(
  id: Option[Long],
  userId: Option[Long],
  toUserId: Option[Long],
  subject: Option[String],
  body: Option[String],
  var interaction: Option[Boolean], // enriched field
  var trusted: Option[Boolean], // enriched field
  createdAt: Date,
  adminMessage: Boolean) extends AbstractMessage {

  def recipientId = toUserId
  def senderId = userId
  def content = List(subject.getOrElse(""), body.getOrElse("")).mkString(" ")
  val public = Some(false)

  def noSpamCheck = adminMessage
}

case class Post(
  id: Option[Long],
  userId: Option[Long],
  title: Option[String],
  body: Option[String],
  tags: Option[String],
  permalink: Option[String],
  public: Option[Boolean],
  createdAt: Option[Date],
  var trusted: Option[Boolean], // enriched field
  updatedAt: Option[Date]) extends AbstractPost {

  // messagelike
  def recipientId = None
  def senderId = userId
  def content = List(
    title,
    body,
    tags,
    permalink).flatten.mkString(" ")

  var interaction: Option[Boolean] = None

  def noSpamCheck = false
}

case class User(
  id: Option[Long],
  firstName: Option[String],
  lastName: Option[String],
  username: Option[String],
  email: Option[String],
  city: Option[String],
  description: Option[String],
  permalink: Option[String],
  ips: Option[List[String]],
  createdAt: Option[Date],
  var trusted: Option[Boolean], // enriched field
  updatedAt: Option[Date]) extends AbstractUser {

  // messagelike
  def recipientId = None
  def senderId = id
  def content = List(
    firstName,
    lastName,
    city,
    description,
    permalink).flatten.mkString(" ")

  var interaction: Option[Boolean] = None
  val public = Some(true)

  def noSpamCheck = false
}


package object readers {
  import com.soundcloud.sketchy.util.readers._
  implicit val affiliationReader    = Json.reads[Affiliation]
  implicit val commentReader        = Json.reads[Comment]
  implicit val favoritingReader     = Json.reads[Favoriting]
  implicit val messageReader        = Json.reads[Message]
  implicit val postReader           = Json.reads[Post]
  implicit val userReader           = Json.reads[User]
}


package object writers {
  import com.soundcloud.sketchy.util.writers._
  implicit val affiliationWriter    = Json.writes[Affiliation]
  implicit val commentWriter        = Json.writes[Comment]
  implicit val favoritingWriter     = Json.writes[Favoriting]
  implicit val messageWriter        = Json.writes[Message]
  implicit val postWriter           = Json.writes[Post]
  implicit val userWriter           = Json.writes[User]

  def serialize(e: Event): String = e match {
    case i: Affiliation   => JSON.json(i)
    case i: Comment       => JSON.json(i)
    case i: Favoriting    => JSON.json(i)
    case i: Message       => JSON.json(i)
    case i: Post          => JSON.json(i)
    case i: User          => JSON.json(i)
    case _ => com.soundcloud.sketchy.events.writers.serialize(e)
  }
}
