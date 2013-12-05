package com.soundcloud.sketchy.util

import scala.actors.Actor

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.dynamicSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import Q.interpolation

import scala.slick.driver.MySQLDriver.backend.{ Database => SlickDatabase }


object DatabaseHealthMonitor {
  protected val health = scala.collection.mutable.Map[SlickDatabase,Boolean]()
}

class DatabaseHealthMonitor {
  import DatabaseHealthMonitor.health

  def isHealthy(db: SlickDatabase) = {
    val storedStatus = health.get(db)
    if (storedStatus.isDefined) {
      storedStatus.get
    } else {
      registerDatabase(db)
      health(db)
    }
  }

  def start { Monitor.start() }

  protected def queryHealth(db: SlickDatabase): Boolean =
    try {
      db.withDynSession {
        val q = sql"SELECT 1".as[Int]
        q.first
      } == 1
    } catch {
      case e: Throwable => false
    }

  protected def registerDatabase(db: SlickDatabase) {
    if (!health.contains(db)) health(db) = queryHealth(db)
  }

  protected object Monitor extends Actor {
    def act() {
      while(true) {
        update
        Thread.sleep(60000)
      }
    }

    def update {
      val databases = health.keySet.toList
      databases.foreach(db => {
        DatabaseHealthMonitor.health(db) = queryHealth(db)
      })
    }
  }

  protected def set(db: SlickDatabase, status: Boolean) {
    health(db) = status
  }

  protected def clear { health.clear() }
}
