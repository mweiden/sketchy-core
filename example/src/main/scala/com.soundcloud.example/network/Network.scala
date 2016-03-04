package com.soundcloud.example.network

import com.soundcloud.example.agent._
import com.soundcloud.example.events._
import com.soundcloud.example.events.writers.serialize
import com.soundcloud.example.ingester._
import com.soundcloud.sketchy.access.SketchyReputations
import com.soundcloud.sketchy.agent._
import com.soundcloud.sketchy.broker.HaBroker
import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events._
import com.soundcloud.sketchy.network._
import com.soundcloud.sketchy.util._
import net.spy.memcached.MemcachedClient


/**
 * Detection network ingests live user events and runs statistics and
 * detectors which emit sketchy signals.
 */
class DetectionNetwork(broker: HaBroker,
                       memory: MemcachedClient,
                       sketchy: SketchyReputations,
                       shortTermCtx: ContextCfg,
                       longTermCtx: ContextCfg,
                       classifier: Classifier) extends DetectionNetworkCfg(broker) {

  val batchCtx  = new NamedCacheContext[BatchStatistics]("ba", memory, longTermCtx)
  val bulkCtx   = new NamedCacheContext[BulkStatistics]("b", memory, shortTermCtx)
  val junkCtx   = new NamedCacheContext[JunkStatistics]("j", memory, shortTermCtx)
  val reportCtx = new NamedCacheContext[SpamReportStatistics]("sr", memory, longTermCtx)

  val edgeHistoryCtx =
    new CountingContext("ch", memory, longTermCtx.copy(numBuckets = 24))
  val burstCountCtx =
    new CountingContext("cc", memory, shortTermCtx.copy(numBuckets = 24))

  val signalEmitterAgent =
    new SignalEmitterAgent(broker, "sketchy", "Signal") with ActorPropagation
  val ingestorLoggingAgent =
    new LoggingAgent("ingestors", serialize) with ActorPropagation
  val signalLoggingAgent =
    new LoggingAgent("signals", serialize) with ActorPropagation
  val messageLikeEnrichAgent =
    new MessageLikeEnrichAgent(sketchy) with ActorPropagation
  val edgeChangeAgent =
    new EdgeChangeAgent(edgeHistoryCtx) with ActorPropagation
  val batchStatisticsAgent =
    new BatchStatisticsAgent(batchCtx) with ActorPropagation
  val bulkStatisticsAgent =
    new ExampleBulkStatisticsAgent(bulkCtx) with ActorPropagation
  val junkStatisticsAgent =
    new ExampleJunkStatisticsAgent(junkCtx, classifier) with ActorPropagation
  val spamReportStatisticsAgent =
    new SpamReportStatisticsAgent(reportCtx) with ActorPropagation
  val bulkDetectorAgent =
    new BulkDetectorAgent(bulkCtx) with ActorPropagation
  val junkDetectorAgent =
    new JunkDetectorAgent(junkCtx) with ActorPropagation
  val spamReportDetectorAgent =
    new SpamReportDetectorAgent(reportCtx) with ActorPropagation
  val rateLimiterAgent =
    new ExampleRateLimiterAgent(burstCountCtx) with ActorPropagation
  val blacklistAgent =
    new BlacklistAgent() with ActorPropagation

}

/**
 * Base configuration of detection network.
 */
abstract class DetectionNetworkCfg(broker: HaBroker) extends Network {
  import com.soundcloud.sketchy.events.readers._
  import com.soundcloud.example.events.readers._

  val network = "example"

  /*
   * ingest user events from RabbitMQ
   */
  val affiliationCreateIngester = new RabbitUserEventIngester[Affiliation](
    broker, network, UserEvent.Create, "Affiliation")
  val affiliationDestroyIngester = new RabbitUserEventIngester[Affiliation](
    broker, network, UserEvent.Destroy, "Affiliation")
  val favoritingCreateIngester = new RabbitUserEventIngester[Favoriting](
    broker, network, UserEvent.Create, "Favoriting")
  val favoritingDestroyIngester = new RabbitUserEventIngester[Favoriting](
    broker, network, UserEvent.Update, "Favoriting")

  val commentCreateIngester = new RabbitUserEventIngester[Comment](
    broker, network, UserEvent.Create, "Comment")
  val messageCreateIngester = new RabbitUserEventIngester[Message](
    broker, network, UserEvent.Create, "Message")
  val postCreateIngester = new RabbitUserEventIngester[Post](
    broker, network, UserEvent.Create, "Post")
  val userUpdateIngester = new RabbitUserEventIngester[User](
    broker, network, UserEvent.Update, "User")

  /*
   * optionally ingest user events from HTTP
   */
  val spamReportCreateIngester = new HTTPUserEventIngester[SpamReport](
    network, UserEvent.Create, "SpamReport")

  val timerIngester = new BatchCheckIngester

  /*
   * agents
   */
  val signalEmitterAgent: SignalEmitterAgent

  // The enrich agents directly depend on MySQL. The agent base class is used
  // so that they can be replaced in the test network.
  val messageLikeEnrichAgent: Agent
  val edgeChangeAgent: Agent

  val batchCtx: Context[BatchStatistics]
  val bulkCtx: Context[BulkStatistics]
  val junkCtx: Context[JunkStatistics]
  val reportCtx: Context[SpamReportStatistics]

  val batchStatisticsAgent: BatchStatisticsAgent
  val bulkStatisticsAgent: BulkStatisticsAgent

  val blacklistAgent: BlacklistAgent

  val junkStatisticsAgent: JunkStatisticsAgent
  val spamReportStatisticsAgent: SpamReportStatisticsAgent

  val bulkDetectorAgent: BulkDetectorAgent
  val rateLimiterAgent: ExampleRateLimiterAgent

  val junkDetectorAgent: JunkDetectorAgent
  val spamReportDetectorAgent: SpamReportDetectorAgent

  val ingestorLoggingAgent: Agent
  val signalLoggingAgent: Agent

  def enable() {
    // edge-likes -> edge-like enrichment -> detection
    affiliationCreateIngester -> edgeChangeAgent
    affiliationDestroyIngester -> edgeChangeAgent
    favoritingCreateIngester -> edgeChangeAgent
    favoritingDestroyIngester -> edgeChangeAgent

    edgeChangeAgent -> rateLimiterAgent

    // messagelike -> detectors
    commentCreateIngester -> messageLikeEnrichAgent
    messageCreateIngester -> messageLikeEnrichAgent
    postCreateIngester -> messageLikeEnrichAgent
    userUpdateIngester -> messageLikeEnrichAgent

    userUpdateIngester -> blacklistAgent

    messageLikeEnrichAgent -> bulkStatisticsAgent
    messageLikeEnrichAgent -> junkStatisticsAgent

    bulkStatisticsAgent -> batchStatisticsAgent
    timerIngester -> batchStatisticsAgent

    batchStatisticsAgent -> bulkDetectorAgent
    bulkStatisticsAgent -> bulkDetectorAgent
    junkStatisticsAgent -> junkDetectorAgent

    // spam reports -> detectors
    spamReportCreateIngester -> spamReportDetectorAgent

    // detectors -> emitter
    bulkDetectorAgent -> signalEmitterAgent
    rateLimiterAgent -> signalEmitterAgent
    junkDetectorAgent -> signalEmitterAgent
    spamReportDetectorAgent -> signalEmitterAgent
    blacklistAgent -> signalEmitterAgent

    // ingesters -> logger
    affiliationCreateIngester -> ingestorLoggingAgent
    affiliationDestroyIngester -> ingestorLoggingAgent
    favoritingCreateIngester -> ingestorLoggingAgent
    favoritingDestroyIngester -> ingestorLoggingAgent

    // emitter -> logger
    signalEmitterAgent -> signalLoggingAgent

    // enable emitter/logger
    ingestorLoggingAgent.enable()
    signalLoggingAgent.enable()
    signalEmitterAgent.enable()

    // enable detectors
    bulkDetectorAgent.enable()
    rateLimiterAgent.enable()
    junkDetectorAgent.enable()
    spamReportDetectorAgent.enable()
    blacklistAgent.enable()

    // enable statistics agent
    batchStatisticsAgent.enable()
    bulkStatisticsAgent.enable()
    junkStatisticsAgent.enable()
    spamReportStatisticsAgent.enable()

    // enable enrichers
    messageLikeEnrichAgent.enable()
    edgeChangeAgent.enable()

    // enable ingesters
    affiliationCreateIngester.enable()
    affiliationDestroyIngester.enable()
    favoritingCreateIngester.enable()
    favoritingDestroyIngester.enable()

    commentCreateIngester.enable()
    messageCreateIngester.enable()
    userUpdateIngester.enable()

    // HTTP ingestion works differently!
    spamReportCreateIngester.enable()

    timerIngester.enable()
  }
}
