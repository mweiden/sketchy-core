package com.soundcloud.sketchy.util

import java.io.PrintStream
import java.util.Date
import org.apache.commons.lang.exception.ExceptionUtils

import com.soundcloud.sketchy.monitoring.Instrumented

import scala.collection.immutable.TreeMap


object Logging {
  lazy val log = new Logger()
}

trait Logging {
  val log = Logging.log
}

/**
 * Rudimentary Bark syslog bridge compliant logging.
 *
 */
class Logger(
  emailExceptions: Boolean = true,
  prependTimeInfo: Boolean = false) extends Instrumented {

  var mailer: Option[Mailer] = None

  def metricsSubtypeName: Option[String] = None
  def metricsTypeName: String = "logger"

  def debug(message: String) { log('DEBUG, message) }
  def info(message: String)  { log('INFO,  message) }
  def warn(message: String)  { log('WARN,  message) }
  def error(message: String) { log('ERROR, message, System.err) }
  def fatal(message: String) { log('FATAL, message, System.err) }

  val digest = new Digest(limit = 2)

  def error(e: Throwable, message: String) {
    val exceptionType = e.toString.split(':').head

    val description = message + exception(e)

    counter.newPartial()
      .labelPair("exception", exceptionType)
      .apply().increment()
    error(description)

    if(emailExceptions && digest.allow(exceptionType)) {
      email(description)
    }
  }

  // log a formatted string to a given descriptor. can add headers here.
  def log(
    level: Symbol,
    message: String,
    out: PrintStream = new PrintStream(System.out, true, "UTF-8"),
    meta: List[(String, String)] = Nil,
    date: Date = new Date) {
    out.println(format(level, message, date, meta))
    out.flush()
  }

  // produce barkish loglines
  private def format(
    level: Symbol,
    message: String,
    date: Date,
    meta: List[(String, String)]): String = {

    val line = if (prependTimeInfo) {
      "%tF %tT%tz %s".format(date, date, date, message)
    } else {
      message
    }

    if (meta.isEmpty) {
      "%s %s".format(level.name, line)
    } else {
      "%s[%s] %s".format(level.name, metaStr(meta), line)
    }
  }

  private def metaStr(meta: List[(String, String)]): String =
    return meta.map(x => x._1 + "=\"" + x._2 + "\"").mkString(",")

  private def exception(e: Throwable): String =
    ExceptionUtils.getRootCauseStackTrace(e).foldLeft("")((a, b) => a + "\n! " + b)

  /**
   * Failure to send emails is handled and logged.
   */
  private def email(message: String) {
    mailer match {
      case Some(mailer) => {
        try {
          mailer.send(message)
        } catch {
          case e: Throwable => {
            error("failed to send exception email for " + exception(e))
          }
          case _ => {
            error("failed to send exception email")
          }
        }
      }
      case None =>
    }
  }

  private val counter = prometheusCounter("exception")
}
