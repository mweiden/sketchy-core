package com.soundcloud.sketchy.agent

import org.apache.log4j.Logger

import scala.actors.Actor

import com.soundcloud.sketchy.monitoring.Instrumented
import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.Event
import com.soundcloud.sketchy.util.Logging

/**
 * Agents process and emit events in the Network.
 * They have names that you should call them by (to be nice).
 */
abstract class Agent extends Notifying with Actor with Instrumented {

  def metricsNameArray   = this.getClass.getSuperclass.getName.split('.')
  def metricsTypeName    = metricsNameArray(metricsNameArray.length - 1)
  def metricsSubtypeName = Some(metricsNameArray(metricsNameArray.length - 2))
  val loggerName = this.getClass.getName
  lazy val logger = Logger.getLogger(loggerName)

  def on(event: Event): Seq[Event]
  def enable(): Boolean = true
  def act() {
    while (true) {
      receive {
        case event: Event =>
           try { on(event) } catch {
             case e: Throwable => Logging.log.error(logger,e,"caught out of on(event)")
           }
      }
    }
  }
}

