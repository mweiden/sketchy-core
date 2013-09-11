package com.soundcloud.example.events

import java.util.Date

import com.soundcloud.sketchy.events._


/**
 * User action primitive classes
 *
 * These include the information encoded in a user action. There are two main
 * types: EdgeLike actions and MessageLike actions.
 */
case class Affiliation(
    id: Option[Int],
    userId: Option[Int],
    followeeId: Option[Int],
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
  id: Option[Int],
  userId: Option[Int],
  body: Option[String],
  itemId: Option[Int],
  itemKind: Option[String],
  itemAuthorId: Option[Int],
  var interaction: Option[Boolean], // enriched field
  var trusted: Option[Boolean], // enriched field
  createdAt: Date) extends AbstractComment {

  def senderId = userId
  def recipientId = itemAuthorId
  def content = body.getOrElse("")

  def noSpamCheck = trusted.getOrElse(false)
}

case class Favoriting(
  id: Option[Int],
  userId: Option[Int],
  itemId: Option[Int],
  itemKind: Option[String],
  createdAt: Date,
  deletedAt: Date) extends AbstractFavoriting with DeleteOnUpdate {

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
  id: Option[Int],
  userId: Option[Int],
  toUserId: Option[Int],
  subject: Option[String],
  body: Option[String],
  var interaction: Option[Boolean], // enriched field
  var trusted: Option[Boolean], // enriched field
  createdAt: Date,
  adminMessage: Boolean) extends AbstractMessage {

  def recipientId = toUserId
  def senderId = userId
  def content = List(subject.getOrElse(""), body.getOrElse("")).mkString(" ")

  def noSpamCheck = adminMessage
}

case class Post(
  id: Option[Int],
  userId: Option[Int],
  title: Option[String],
  body: Option[String],
  tags: Option[String],
  permalink: Option[String],
  public: Boolean,
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
  id: Option[Int],
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

  def noSpamCheck = false
}

