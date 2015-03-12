package com.soundcloud.sketchy.util

import java.io.PrintStream
import java.util.Date
import org.apache.commons.lang.exception.ExceptionUtils

import com.soundcloud.sketchy.monitoring.Instrumented
import org.apache.log4j.Logger

import scala.collection.immutable.TreeMap

object Logging {
  lazy val log = new CustomLogger()
}

trait Logging {
  val log = Logging.log
}

/**
 *
 *
 */
class CustomLogger(
  emailExceptions: Boolean = true
  ) extends Instrumented {

  var mailer: Option[Mailer] = None

  def metricsSubtypeName: Option[String] = None
  def metricsTypeName: String = "logger"

  def debug(log:Logger, message: String) { log.debug(message)}
  def info(log:Logger, message: String)  { log.info(message) }
  def warn(log:Logger, message: String)  { log.warn(message) }
  def fatal(log:Logger, message: String) { log.fatal(message)}

  val digest = new Digest(limit = 2)

  def error(log:Logger,e: Throwable, message: String) {
    val exceptionType = e.toString.split(':').head

    val description = message + exception(e)

    counter.newPartial()
      .labelPair("exception", exceptionType)
      .apply().increment()
    error(log,description)

    if(emailExceptions && digest.allow(exceptionType)) {
      email(log,description)
    }
  }

  private def error(log:Logger, message: String) { log.error(message) }

  private def exception(e: Throwable): String =
    ExceptionUtils.getRootCauseStackTrace(e).foldLeft("")((a, b) => a + "\n! " + b)

  /**
   * Failure to send emails is handled and logged.
   */
  private def email(log:Logger,message: String) {
    mailer match {
      case Some(mailer) => {
        try {
          mailer.send(message)
        } catch {
          case e: Throwable => {
            error(log,"failed to send exception email for " + exception(e))
          }
          case _ => {
            error(log,"failed to send exception email")
          }
        }
      }
      case None =>
    }
  }

  private val counter = prometheusCounter("exception")
}
