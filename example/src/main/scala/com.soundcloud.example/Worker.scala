package com.soundcloud.example.network

import System.{ getProperty => property }
import java.util.{ Locale, TimeZone }
import org.slf4j.{LoggerFactory,Logger}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ ServletContextHandler, ServletHolder }
import org.eclipse.jetty.webapp.WebAppContext

import com.soundcloud.sketchy.access.SketchyAccess
import com.soundcloud.sketchy.broker.{ HaRabbitBroker, HaBroker }
import com.soundcloud.sketchy.context.{ ContextCfg, Ring }
import com.soundcloud.sketchy.util._

import com.soundcloud.example.util.SVMClassifier

import io.prometheus.client.exporter.MetricsServlet

/**
 * Starts up the sketchy network
 */
object Worker {

  val loggerName = this.getClass.getName
  lazy val logger = LoggerFactory.getLogger(loggerName)

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

    network.enable()

    logger.info("Starting servlets on port %s".format(property("web.port")))
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
    logger.info("HTTP server for '" + property("network.name") + "' up")
    server.join()
  }
}
