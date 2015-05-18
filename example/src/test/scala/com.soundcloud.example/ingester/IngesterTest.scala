package com.soundcloud.example.ingester

import org.scalatest.{ FlatSpec, BeforeAndAfterEach }
import org.scalatra.test.scalatest._

import java.util.Date
import java.text.SimpleDateFormat

import com.soundcloud.sketchy.agent.Agent
import com.soundcloud.sketchy.events._

import com.soundcloud.example.SpecHelper
import com.soundcloud.example.events._

import play.api.libs.json.Json


class IngesterTest extends FlatSpec with ScalatraSuite with BeforeAndAfterEach {
  import com.soundcloud.example.events.readers._
  import com.soundcloud.sketchy.events.readers._

  val servlet = new HTTPUserEventIngester[Affiliation](
    "test",
    UserEvent.Create,
    "affiliation")

  var receiver = new Agent{
      val received = scala.collection.mutable.Queue[Event]()

      def on(event: Event): Seq[Event] = {
        received += event
        Nil
      }
    }

  servlet -> receiver
  val contentType = Map("Content-Type" -> "application/json")

  addServlet(servlet , "/*")

  override def afterEach() {
    receiver.received.clear()
  }

  behavior of "the HTTP ingester"

  it should "POST /network/action/key returns 202 for valid JSON" in {
    val (json, expected) = affiliationJson(1, 2, 3, new Date, true)

    post("/test/create/affiliation", json, contentType) {
      assert(status === 202)
    }
  }

  it should "POST /network/action/key returns 400 for malformed JSON" in {
    val json = """{"content":"this is some garbage"}"""

    post("/test/create/affiliation", json, contentType) {
      assert(status === 400)
    }
  }

  it should "POST /network/action/key should emit an event parsed from JSON" in {
    val date = dateFormat.parse("2013/09/10 21:42:03 +0000")
    val (json, expected) = affiliationJson(1, 2, 3, date, true)

    post("/test/create/affiliation", json, contentType) {
      assert(receiver.received.length === 1)
      assert(receiver.received.head === expected)
    }
  }

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss ZZZZZ")

  implicit val affWriter = Json.writes[Affiliation]

  def affiliationJson(
    id: Int,
    userId: Int,
    followeeId: Int,
    createdAt: Date,
    recommended: Boolean): (String,Affiliation) = {
    val aff = Affiliation(
      Some(id),
      Some(userId),
      Some(followeeId),
      createdAt,
      Some(recommended))
    (JSON.json(aff), aff)
  }

}

