package com.soundcloud.sketchy.agent


import org.slf4j.{LoggerFactory, Logger}

import scala.actors.Actor

import com.soundcloud.sketchy.network.Notifying
import com.soundcloud.sketchy.events.Event
import com.soundcloud.sketchy.util.Exceptions
/**
 * Agents process and emit events in the Network.
 * They have names that you should call them by (to be nice).
 */
abstract class Agent extends Notifying with Actor {

  def metricsNameArray = this.getClass.getSuperclass.getName.split('.')
  val metricsName      = metricsNameArray(metricsNameArray.length - 2).toLowerCase
  val metricsSubtypeName = metricsNameArray(metricsNameArray.length - 1)

  lazy val logger = LoggerFactory.getLogger(metricsSubtypeName)

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

