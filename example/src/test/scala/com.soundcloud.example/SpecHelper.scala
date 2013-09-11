package com.soundcloud.example

import scala.io.Source.fromFile
import org.scalatest.{ BeforeAndAfter, FlatSpec }
import net.spy.memcached._

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.network.DirectPropagation
import com.soundcloud.sketchy.agent.limits.{ BurstLimit, BurstLimits }
import com.soundcloud.sketchy.util.{
  Classifier,
  Tokenizer,
  HttpClient
}

import com.soundcloud.example.network.Worker
import com.soundcloud.example.events._
import com.soundcloud.example.util.SVMClassifier
import com.soundcloud.example.agent._
import com.soundcloud.example.agent.limits.ExampleBurstLimits


object SpecHelper {
  /**
   * Lower maximum burst limits for specified limits
   */
  def limitsWithMax(newMax: Int): BurstLimits = {
    new BurstLimits(
      ExampleBurstLimits.defaults.map(limit => limit.copy(max = newMax)))
  }
}

trait SpecHelper extends FlatSpec {

  Worker.localize()

  def blacklistAgent(ip: String) =
    new BlacklistAgent() with DirectPropagation {
      override val http = new HttpClient("mock") {
        override def post(url: String, body: String, isJson: Boolean) =
          if (url.contains(ip))
            (200, "<appears>yes</appears>")
          else
            (200, "nothing")
      }
    }

  /**
   * Fixtures
   */
  val fixturesPath = "example/src/test/resources/fixtures/"

  def brokerFixture(name: String) = fromFile(path("broker", name)).mkString
  def h2db(name: String) = path("db", name)

  /**
   * Fixtures setup
   */

  def fixtures(kind: String, name: String): String =
    fromFile(path(kind, name)).mkString

  def bulkStats(name: String): Seq[BulkStatistics] =
    fixtures("stat", name).split('\n').toSeq.map(BulkStatistics.unmarshal _)

  def junkStats(name: String): Seq[JunkStatistics] =
    fixtures("stat", name).split('\n').toSeq.map(JunkStatistics.unmarshal _)

  /**
   * Single element fixtures
   */
  def comment(name: String) =
    Event.fromJson[Comment](brokerFixture("comment." + name))

  def following(name: String) =
    Event.fromJson[Affiliation](brokerFixture("affiliation." + name))

  def unfollowing(name: String) = {
    val aff = Event.fromJson[Affiliation](brokerFixture("affiliation." + name))
    aff.action = UserEvent.Destroy
    aff
  }

  def message(name: String) =
    Event.fromJson[Message](brokerFixture("message." + name))

  def spamReport(name: String) =
    Event.fromJson[Comment](brokerFixture("spam_report." + name))

  def user(name: String) =
    Event.fromJson[User](brokerFixture("user." + name))

  def bulkStat(name: String) =
    bulkStats(name).head

  def fixture(kind: String, name: String) =
    fixtures(kind, name).head

  def junkStat(name: String) =
    junkStats(name).head

  def svmClassifier(name: String): SVMClassifier =
    new SVMClassifier(path("model",name))

  private def path(kind: String, name: String): String =
    kind match {
      case "broker" => fixturesPath + "broker/" + name + ".json"
      case "model" => fixturesPath + "model/" + name + ".nak"
      case "stat" => fixturesPath + "stat/" + name + ".txt"
      case "signal" => fixturesPath + "signal/" + name + ".json"
      case "log" => fixturesPath + "log/" + name + ".log"
      case "db" => fixturesPath + "db/" + name + ".sql"
    }
}
