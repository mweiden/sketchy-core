package com.soundcloud.sketchy.agent


import org.slf4j.{LoggerFactory, Logger}

import scala.actors.Actor

import com.soundcloud.sketchy.monitoring.Instrumented
import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.Event
import com.soundcloud.sketchy.util.Exceptions
/**
 * Agents process and emit events in the Network.
 * They have names that you should call them by (to be nice).
 */
abstract class Agent extends Notifying with Actor with Instrumented {

  def metricsNameArray = this.getClass.getSuperclass.getName.split('.')
  val metricsName      = metricsNameArray(metricsNameArray.length - 2).toLowerCase
  override val metricsSubtypeName = Some(metricsNameArray(metricsNameArray.length - 1).toLowerCase)

  val loggerName = metricsSubtypeName.get
  lazy val logger = LoggerFactory.getLogger(loggerName)

  def on(event: Event): Seq[Event]
  def enable(): Boolean = true
  def act() {
    while (true) {
      receive {
        case event: Event =>
           try { on(event) } catch {
             case e: Throwable => Exceptions.report(e)
                                  logger.error("caught out of on(event)",e)
           }
      }
    }
  }
}

