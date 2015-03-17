package com.soundcloud.sketchy.util

import java.io.PrintStream
import java.util.Date
import org.apache.commons.lang.exception.ExceptionUtils

import com.soundcloud.sketchy.monitoring.Instrumented
import org.apache.log4j.spi.LoggerFactory
import org.apache.log4j.{LogManager, Logger}

import scala.collection.immutable.TreeMap


object Logging {


  def getLogger(name: String): Logging = {

    println("Repository!!!!!!!!!!!!!!!!!"+ LogManager.getLoggerRepository.getClass.getName)


    val log = LogManager.getLogger(name, new LoggerFactory() {
      override def makeNewLoggerInstance(name: String): Logging = {
        println("makeNewLogger!!!!!!!!!!")
        new Logging(name)
      }
    })

    log match {
      case l: Logging => println("Logging class!!!!!!!!!!!! ") ;l
      case l: Logger => println("Logger class!!!!!!!!!!!!"); throw new ClassCastException
      case _ => println("something else!!!!!!!!!!!!!!") ;throw new ClassCastException
    }
  }
}


class Logging(name:String,emailExceptions: Boolean = true) extends Logger(name) with Instrumented {

  var mailer: Option[Mailer] = None

  def metricsSubtypeName: Option[String] = None
  def metricsTypeName: String = "logger"


  val digest = new EmailThrottle(limit = 2)

  def error(message: String,e: Throwable) {
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

  private def metaStr(meta: List[(String, String)]): String =
    return meta.map(x => x._1 + "=\"" + x._2 + "\"").mkString(",")

  private def exception(e: Throwable): String =
    ExceptionUtils.getRootCauseStackTrace(e).foldLeft("")((a, b) => a + "\n! " + b)
  private val counter = prometheusCounter("exception")

}
