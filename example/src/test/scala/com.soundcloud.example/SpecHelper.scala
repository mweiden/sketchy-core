package com.soundcloud.example

import scala.slick.driver.H2Driver.simple.{ Database => SlickDatabase, _ }
import scala.slick.driver.H2Driver.simple.Database.dynamicSession

import com.soundcloud.example.agent._
import com.soundcloud.example.agent.limits.ExampleLimits
import com.soundcloud.example.events._
import com.soundcloud.example.util.{Driver, DatabaseCfg, Database, SVMClassifier}
import com.soundcloud.sketchy.agent.limits.Limits
import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.network.DirectPropagation
import com.soundcloud.sketchy.util.{HttpClient, Time}
import org.scalatest.FlatSpec

import scala.io.Source.fromFile


object SpecHelper {
  /**
   * Lower maximum limits for specified limits
   */
  def limitsWithMax(newMax: Int): Limits = {
    new Limits(
      ExampleLimits.defaults.map(limit => limit.copy(limit = newMax)))
  }
}

trait SpecHelper extends FlatSpec {
  import com.soundcloud.example.events.readers._

  Time.localize()

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
    * H2 Testing driver
    */
  class H2Driver(sqlDump: String) extends Driver {
    val params = "MODE=MySQL;INIT=RUNSCRIPT FROM '%s'".format(sqlDump)

    val name = "org.h2.Driver"

    def uri(cfg: DatabaseCfg): String =
      "jdbc:h2:mem:%s;%s".format(cfg.db, params)
  }

  def dbCfg(fixture: String = "sketchy.h2") = DatabaseCfg(
    "sketchy",
    "",
    "",
    "127.0.0.1",
    "sketchy_production",
    new H2Driver(h2db(fixture)),
    readOnly = false)

  def database() = new Database(List(dbCfg()))

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
    JSON.fromJson(brokerFixture("comment." + name)).get.as[Comment]

  def following(name: String) =
    JSON.fromJson(brokerFixture("affiliation." + name)).get.as[Affiliation]

  def unfollowing(name: String) = {
    val aff = JSON.fromJson(brokerFixture("affiliation." + name)).get.as[Affiliation]
    aff.action = UserEvent.Destroy
    aff
  }

  def message(name: String) =
    JSON.fromJson(brokerFixture("message." + name)).get.as[Message]

  def spamReport(name: String) =
    JSON.fromJson(brokerFixture("spam_report." + name)).get.as[Comment]

  def user(name: String) =
    JSON.fromJson(brokerFixture("user." + name)).get.as[User]

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
