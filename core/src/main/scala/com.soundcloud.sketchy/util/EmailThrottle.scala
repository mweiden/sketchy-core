package com.soundcloud.sketchy.util

import System.{ currentTimeMillis => now }

import org.apache.log4j.spi.{LoggingEvent, TriggeringEventEvaluator}


class EmailThrottle(limit: Int, intervalTimeout: Long) extends TriggeringEventEvaluator {

  protected var countThisInterval = Map[String,Int]()
  protected var thisIntervalTimestamp: Long = now
//  val limit = 2
//  val intervalTimeout: Long = 60L * 60L * 1000L

  def this()= this(3, 60L * 60L * 1000L)
  def this(limit:Int) = this(limit,60L * 60L * 1000L)

  override def isTriggeringEvent(event: LoggingEvent): Boolean ={
    val id = getId(event.getThrowableStrRep)
    if (id.isEmpty) false else allow(id)
  }


  def allow(identifier: String) = this.synchronized {

    // clear the interval if it is old
    val age = now - thisIntervalTimestamp

    if (age >= intervalTimeout) {
      countThisInterval = Map[String,Int]()
      thisIntervalTimestamp = now
    }

    // see if the current request passes
    val countOpt = countThisInterval.get(identifier)

    val count = if (countOpt.isDefined) {
      countThisInterval += identifier -> (countOpt.get + 1)
      countThisInterval(identifier)
    } else {
      countThisInterval += identifier -> 0
      0
    }

    if (count < limit) true else false
  }

  private def getId(mess:Array[String]) ={
    if (mess != null && mess.size > 0) mess(0).split(":").head else ""
  }

}


