package com.soundcloud.example.network

import System.{ getProperty => property }
import java.util.{ Locale, TimeZone }
import org.apache.log4j.Logger
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ ServletContextHandler, ServletHolder }
import org.eclipse.jetty.webapp.WebAppContext

import com.soundcloud.sketchy.access.SketchyAccess
import com.soundcloud.sketchy.broker.{ HaRabbitBroker, HaBroker }
import com.soundcloud.sketchy.context.{ ContextCfg, Ring }
import com.soundcloud.sketchy.util._

import com.soundcloud.example.util.SVMClassifier

import io.prometheus.client.utility.servlet.MetricsServlet

/**
 * Starts up the sketchy network
 */
object Worker extends Logging {

  val loggerName = this.getClass.getName
  lazy val logger = Logger.getLogger(loggerName)

  def main(args: Array[String]) {
    Time.localize()

    val broker: HaBroker = new HaRabbitBroker(
      property("broker.ha.publish"),
      list(property("broker.ha.consume")))

    val memory = MemcachedConnectionPool.get(
      list(property("memory.hosts")).mkString(" "))

    val dbSketchy = new Database(
      List(
        DatabaseCfg(
          "sketchy",
          property("db.sketchy.slave.user"),
          property("db.sketchy.slave.password"),
          property("db.sketchy.slave.host"),
          property("db.sketchy.slave.name")),
        DatabaseCfg(
          "sketchy",
          property("db.sketchy.master.user"),
          property("db.sketchy.master.password"),
          property("db.sketchy.master.host"),
          property("db.sketchy.master.name"))
      )
    )

    val sketchy = new SketchyAccess(dbSketchy)

    val classifier = new SVMClassifier(property("model.junk.path"))

    val shortTermCtx = ContextCfg(
      ttl = property("context.ttl.short").toInt,
      numBuckets = property("context.numbuckets.short").toInt,
      slack = property("context.slack").toInt,
      fragLimit = property("context.fragLimit").toDouble,
      blockingDelete = property("context.blockingDelete").toBoolean)

    val longTermCtx = ContextCfg(
      ttl = property("context.ttl.long").toInt,
      numBuckets = property("context.numbuckets.long").toInt,
      slack = property("context.slack").toInt,
      fragLimit = property("context.fragLimit").toDouble,
      blockingDelete = property("context.blockingDelete").toBoolean)

    val network = property("network.name") match {
      case "example" => new DetectionNetwork(
        broker,
        memory,
        sketchy,
        shortTermCtx,
        longTermCtx,
        classifier)
      case _ => throw new Exception("No valid network")
    }

    Logging.log.mailer = Some(new Mailer(
      subject = "sketchy network exception",
      recipient = property("exceptions.recipient"),
      sender = property("exceptions.sender")))

    network.enable()

    Logging.log.info(logger,"Starting servlets on port %s".format(property("web.port")))
    serve(property("web.port").toInt)
  }

  private def list(args: String): List[String] =
    args.trim().split(",").filter(_ != "").toList

  private def serve(port: Int) {
    System.setProperty("org.mortbay.log.class", "org.mortbay.log.StdErrLog")

    val server = new Server(port)

    val web = new WebAppContext("example/src/main/webapp", "/")
    server.setHandler(web)
    web.addServlet(new ServletHolder(new MetricsServlet()), "/metrics")
    server.start()
    Logging.log.info(logger,"HTTP server for '" + property("network.name") + "' up")
    server.join()
  }
}
