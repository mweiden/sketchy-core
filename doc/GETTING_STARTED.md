# Getting started

You can start to build your own Sketchy network by understanding two basic components: agents, and their network support. For convenience, to implement a network using the core project's packages, go through the steps that follow.

## Understanding Sketchy agents and networks

Sketchy's core project contains interfaces for event types and
event-processing networks, and access to utilities such as MySQL, Memcached,
and event serialization. You can use them to build a customized Sketchy
network for your web application.

Simply stated, Sketchy _agents_ process events that stem from your web
application. Sketchy agents either produce, transform, or consume events.
Agents from the [example](/example/) project illustrate each behavior.
In the example, _ingester_ agents are producers that translate events, which
are received from a web application, into events inside the network. _Signal
emmitters_ agents are consumers that receive events, which are produced by the
network, and report them back to a client application. _Enrichment_ agents are
transformers that take information that is related to an event from other
sources such as MySQL, and add that information to the event.

The results of each agent's work are passed to other agents along the directed
edges that you define in a Sketchy network.

You can use the network diagram that follows to build a network of agents that
both detects spam messages and limits the rate at which users can send
messages:

![message example](https://github.com/soundcloud/sketchy-core/blob/master/config/img/message.png?raw=true)

Messages from the event source are ingested from AMQP or an HTTP endpoint and
are pushed onto the network by a message ingester. The ingester then
propagates the results to both a junk-classifier pipeline
(`junkStatisticsAgent` to `junkDetectorAgent`) and a rate-limiter agent
(`rateLimiterAgent`). If someone violates the agents' rules, a signal event
propagates to the signal emitter agent that will notify the client
application.


## Building your own Sketchy network

It is easy to build your own network and you can use the [example](/example/)
project to understand what a full implementation might look like. Its network
graph depicted in the image that follows shows the interconnection of the
agents:

![example network](https://github.com/soundcloud/sketchy-core/blob/master/config/img/example.png?raw=true)

The example projects performs all of the common tasks described in the
[README](/README.md).

### Building events that represent your data in Sketchy

For Sketchy to process data from your web appication, it must deserialize
incoming data and build it's own representation of that data. In Sketchy this is
performed with the `Event` or `UserEvent` trait defined in the [core events
package](/core/src/main/scala/com.soundcloud.sketchy/events/Events.scala). Case
classes that extend either trait can be instatiated from JSON with matching
fields using the `Event` object's `fromJson` method.

To illustrate consider the following JSON:

```
{
  "id": 100,
  "subject": "buy viagra",
  "created_at": "2012/04/20 06:36:03 +0000",
  "body": "cheap viagra at http://spammer.com/viagra",
  "user_id": 1,
  "to_user_id": 2,
  "admin_message": false
}
```

To ingest messages such as this you can build a case class such as the one
defined in the [example project implementation of core
events](/example/src/main/scala/com.soundcloud.example/events/Events.scala):

```scala
case class Message(
  id: Option[Int],
  userId: Option[Int],
  toUserId: Option[Int],
  subject: Option[String],
  body: Option[String],
  var interaction: Option[Boolean],
  var trusted: Option[Boolean],
  createdAt: Date,
  adminMessage: Boolean) extends AbstractMessage {

  def recipientId = toUserId
  def senderId = userId
  def content = List(subject.getOrElse(""), body.getOrElse("")).mkString(" ")

  def noSpamCheck = adminMessage
}
```

If you have correctly mirrored the structure of your JSON, the `Event` object
should now be able to create objects from JSON messages passed to it by
Sketchy network ingesters.

Sketchy comes with
[ingesters](/core/src/main/scala/com.soundcloud.sketchy/ingester/Ingester.scala)
that accept JSON from both AMQP and HTTP. The example project demonstrates how
to use them in its [ingester package](/example/src/main/scala/com.soundcloud.example/ingester/Ingester.scala).
Note that though the AMQP ingester is designed for a High-Availability (HA)
network topology, the HA network topology is not required.


#### Constructing agents

If the core project does not provide all of the agents that you require, it is
easy to create your own. To do so, extend the abstract `Agent` interface and
implement `def on(event: AgentEvent)`:

```scala
class JunkStatisticsAgent(
  context: Context,
  minSpams: Int = 10,
  maxDist: Double = 0.5) extends Agent {

  def on(event: AgentEvent): Seq[AgentEvent] = {
    event match {
      case UserAction(userId) => detect(userId)
      case _ => Nil
    }
  }
}
```

#### Defining the network edges

To create a network, link your agents together using the `->` operator. The
following code, the network diagram of which was [shown
earlier](#understanding-sketchy-agents-and-networks), rate limits messages and
detects junk content:

```scala
class TestNetwork extends Network {
  val junkContext = new MemoryContext()
  val countingContext = new MemoryContext()
  val classifier = new SVMClassifier("./some/path/model.nak")

  // create the agents
  val messageCreateIngester =
    new RabbitUserEventIngester[Message](broker, network, UserEvent.Create, "Message")

  val junkStatisticsAgent =
    new JunkStatisticsAgent(junkContext, classifier) with ActorPropagation

  val junkDetectorAgent =
    new JunkDetectorAgent(junkContext) with ActorPropagation

  val rateLimiterAgent =
    new RateLimiterAgent(countingContext) with ActorPropagation

  val signalEmitterAgent =
    new SignalEmitterAgent(broker, "sketchy", "Signal") with ActorPropagation

  // link the agents together
  messageCreateIngester -> junkStatisticsAgent
  messageCreateIngester -> rateLimiterAgent

  junkStatisticsAgent -> junkDetectorAgent

  junkDetectorAgent -> signalEmitterAgent
  rateLimiterAgent -> signalEmitterAgent

  def enable() {
    messageCreateIngester.enable()
    junkStatisticsAgent.enable()
    junkDetectorAgent.enable()
    rateLimiterAgent.enable()
    signalEmitterAgent.enable()
  }
}
```

## Installation and deployment

For information on installing Sketchy or declaring it as dependency in your
own project, please see the [Installation Guide](doc/INSTALLATION.md).

