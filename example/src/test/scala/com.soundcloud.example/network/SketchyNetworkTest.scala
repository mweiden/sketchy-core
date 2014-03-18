package com.soundcloud.example.network

import java.util.Date
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import scala.collection.mutable

import com.soundcloud.sketchy.agent._
import com.soundcloud.sketchy.broker.{ HaBroker, MemoryBroker, HaBrokerEnvelope }
import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.ingester.Ingester
import com.soundcloud.sketchy.network._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.util.{ Classifier, Formatting }

import com.soundcloud.example.SpecHelper
import com.soundcloud.example.events._
import com.soundcloud.example.agent._

class TestLabelingAgent extends Agent {
  def on(event: Event): Seq[Event] = Nil
}

class TestEnrichAgent extends Agent {
  def on(event: Event): Seq[Event] = event :: Nil
}

/**
 * Sketchy network with direct propagation for testing
 */
class TestNetwork(
  val broker: HaBroker,
  val signalLoggingAgent: Agent,
  val blacklistAgent: BlacklistAgent,
  val classifier: Classifier) extends DetectionNetworkCfg(broker) {

  val elHistoryCtx = new MemoryContext[Nothing](ContextCfg())
  val countingCtx = new MemoryContext[Nothing](ContextCfg())
  val batchCtx = new MemoryContext[BatchStatistics](ContextCfg())
  val bulkCtx = new MemoryContext[BulkStatistics](ContextCfg())
  val junkCtx = new MemoryContext[JunkStatistics](ContextCfg())
  val reportCtx = new MemoryContext[SpamReportStatistics](ContextCfg())

  val ingestorLoggingAgent =
    new LoggingAgent("ingestors") with ActorPropagation

  val signalEmitterAgent =
    new SignalEmitterAgent(broker, "sketchy", "Signal") with DirectPropagation

  val messageLikeEnrichAgent =
    new TestEnrichAgent with DirectPropagation
  val edgeChangeAgent =
    new EdgeChangeAgent(elHistoryCtx) with DirectPropagation

  val batchStatisticsAgent =
    new BatchStatisticsAgent(batchCtx) with DirectPropagation
  val bulkStatisticsAgent =
    new ExampleBulkStatisticsAgent(bulkCtx) with DirectPropagation
  val junkStatisticsAgent =
    new ExampleJunkStatisticsAgent(junkCtx, classifier) with DirectPropagation
  val spamReportStatisticsAgent =
    new SpamReportStatisticsAgent(reportCtx) with DirectPropagation

  val bulkDetectorAgent =
    new BulkDetectorAgent(bulkCtx) with DirectPropagation
  val rateLimiterAgent =
    new ExampleRateLimiterAgent(countingCtx, SpecHelper.limitsWithMax(1))
      with DirectPropagation
  val junkDetectorAgent =
    new JunkDetectorAgent(junkCtx, 2, 0.7) with DirectPropagation
  val spamReportDetectorAgent =
    new SpamReportDetectorAgent(reportCtx) with DirectPropagation
}

/**
 * Test the Sketchy network
 */
@RunWith(classOf[JUnitRunner])
class SketchyNetworkTest extends FlatSpec with SpecHelper {

  case class Fixture(kind: String, name: String)
  case class FixtureList(kind: String, fixtures: List[Fixture])

  val bulkEvents = List("Comment", "Message", "User")
  val junkEvents = List("Comment", "Message")
  val throttleFixtures = List(
    FixtureList("Affiliation", List(
      Fixture("Affiliation","affiliation.follow"),
      Fixture("Affiliation","affiliation.follow2"))),
    FixtureList("Favoriting", List(
      Fixture("Favoriting","favorite.normal"),
      Fixture("Favoriting","favorite.normal2"))))


  behavior of "The example network"

  it should "not collect any sketchy signals" in {
    new NetworkConfig {
      bulkEvents.foreach(event =>
        produce(UserEvent.Create, event, "%s.junk".format(Formatting.scored(event))))
      assert(collected.isEmpty)
    }
  }

  it should "collect a sketchy signal for a user with a blacklisted IP" in {
    new NetworkConfig {
      produce(UserEvent.Update, "User", "user.junk")
      assert(collected.length === 1)
      assert(collected.head.kind === "User")
      assert(collected.head.detector === "Blacklist")
    }
  }

  junkEvents.foreach(event => {
    it should "collect a sketchy signal with detected junk %s".format(Formatting.scored(event)) in {
      new NetworkConfig {
        1.to(3).foreach { i =>
          produce(UserEvent.Create, event, "%s.junk".format(Formatting.scored(event))) }

        assert(collected.length === 1)
        val actual = collected.head

        assert(actual.detector == "Junk")
        assert(actual.kind == event)
      }
    }
  })

  bulkEvents.foreach(event => {
    it should "collect a sketchy signal with detected bulk %s".format(Formatting.scored(event)) in {
      new NetworkConfig {
        1.to(6).foreach { i =>
          produce(
            if (event == "User") UserEvent.Update else UserEvent.Create,
            event, "%s.junk".format(Formatting.scored(event)))
          }

        val bulks = collected.filter(_.detector == "Bulk")
        assert(bulks.size === 1)
        val actual = bulks.head

        assert(actual.detector == "Bulk")
        assert(actual.kind == event)
      }
    }
  })

  throttleFixtures.map{ case FixtureList(actionKind, list) => {
    it should "throttle %s link events".format(actionKind) in {
      new NetworkConfig {
        list.foreach( conf =>
          produce(UserEvent.Create, conf.kind, conf.name))

        assert(collected.length === 1)
        val actual = collected.head
        assert(actual.detector.contains("Rate"))
        assert(actual.kind === actionKind)
      }
    }
  }}

  throttleFixtures.map{ case FixtureList(actionKind, list) => {
    it should "throttle derived %s relink events".format(actionKind) in {
      new NetworkConfig {
        val (key, fixture) = (list.head.kind, list.head.name)

        produce(UserEvent.Create, key, fixture)
        produce(UserEvent.Create, key, fixture)
        produce(UserEvent.Create, key, fixture)

        if (collected.isEmpty) {
          fail("did not collect any agent events")
        } else {
          val actual = collected.last
          assert(actual.detector.contains("Rate"))
          assert(actual.kind === actionKind)
          assert(actual.detector.contains("Relink"))
        }
      }
    }
  }}

  it should "generate formatted metrics names" in {
    new NetworkConfig {
      assert(sketchy.bulkDetectorAgent.metricsGroupName === "sketchy.test.agent")
      assert(sketchy.bulkDetectorAgent.metricsTypeName === "BulkDetectorAgent")

      assert(sketchy.bulkDetectorAgent.metricsName === "test_agent_total")
      assert(sketchy.bulkDetectorAgent.timerName === "test_agent_timer")

      assert(sketchy.affiliationCreateIngester.metricsGroupName === "sketchy.test.ingester")
      assert(sketchy.affiliationCreateIngester.metricsTypeName === "RabbitUserEventIngester")
    }
  }

  /**
   * Internal Helpers
   */
  val junkClassifier = svmClassifier("junk")

  trait NetworkConfig {
    val blacklist = blacklistAgent("192.168.1.1")
    val broker = new MemoryBroker()
    val sketchy = new TestNetwork(broker, collector, blacklist, junkClassifier)
    val collected = new mutable.Queue[SketchySignal]()

    broker.consumer.subscribe("test.q", "sketchy", "Signal", (x: HaBrokerEnvelope) => null)
    sketchy.enable()

    def produce(action: Action, key: String, fixture: String) {
      val route =
        "live.event.%s#%s".format(action.toString.toLowerCase, key)
      broker.produce(route, brokerFixture(fixture))
    }

    def collector = new Agent {
      def on(t: Event) =
        t match {
          case s: SketchySignal => collected += s; Nil
          case _ => Nil
        }
    }
  }

}

