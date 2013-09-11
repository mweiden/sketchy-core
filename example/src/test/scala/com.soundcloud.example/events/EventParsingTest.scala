package com.soundcloud.example.events

import java.text.SimpleDateFormat
import java.util.Date
import org.scalatest.FlatSpec
import scala.tools.nsc.io.Streamable

import com.soundcloud.example.events._
import com.soundcloud.sketchy.events._

import com.soundcloud.example.SpecHelper


/**
 * Tests all input stream event parsing.
 */
class UserEventParsingTest extends FlatSpec with SpecHelper {
  behavior of "The stream user event parser"

  val simple = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss ZZZZZ")

  it should "parse JSON message" in {
    val json = fixtures("broker", "message.junk").mkString
    val message = Event.fromJson[Message](json)

    assert(message.id.get === 100)
    assert(message.userId.getOrElse(0) === 1)
    assert(message.subject === Some("buy viagra"))
    assert(message.body === Some("cheap viagra at http://spammer.com/viagra"))
    assert(message.adminMessage === false)
    assert(message.createdAt === simple.parse("2012/04/20 06:36:03 +0000"))
  }

  it should "parse JSON comment" in {
    val json = fixtures("broker", "comment.created").mkString
    val comment = Event.fromJson[Comment](json)

    assert(comment.id.get === 55023212)
    assert(comment.body === Some("CHECK THIS SITE freeloopsandsamples.blogspot.com\n"))
    assert(comment.itemId === Some(44932005))
    assert(comment.userId === Some(1))
    assert(comment.createdAt === simple.parse("2012/05/01 08:34:26 +0000"))
  }

  it should "parse JSON affiliation" in {
    val json = fixtures("broker", "affiliation.follow").mkString

    val affiliation = Event.fromJson[Affiliation](json)

    assert(affiliation.id.get === 55023212)
    assert(affiliation.userId.getOrElse(0) === 12)
    assert(affiliation.followeeId.getOrElse(0) === 88)
    assert(affiliation.createdAt === simple.parse("2012/05/01 08:34:26 +0000"))
  }

  it should "parse JSON spam report" in {
    val json = fixtures("broker", "spam_report.comment").mkString
    val report = Event.fromJson[SpamReport](json)

    assert(report.id.get === 16668)
    assert(report.reporterId === 42)
    assert(report.spammerId === 23)
    assert(report.originId === 100)
    assert(report.originType === "Comment")
    assert(report.spamPublishedAt === simple.parse("2012/05/29 00:37:11 +0000"))
    assert(report.createdAt === simple.parse("2012/08/23 17:41:109 +0000"))
    assert(report.updatedAt === simple.parse("2012/08/23 17:41:109 +0000"))
  }

  it should "parse enriched JSON spam report" in {
    val json = fixtures("broker", "spam_report.enriched").mkString
    val report = Event.fromJson[SpamReport](json)

    assert(report.lastSignaledAt === Some(simple.parse("2012/05/30 08:22:03 +0000")))
  }
}
