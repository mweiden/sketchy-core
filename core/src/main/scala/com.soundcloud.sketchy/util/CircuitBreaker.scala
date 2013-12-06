package com.soundcloud.sketchy.util

import java.util.Date


object CircuitBreaker {

  case class Retry (
    startDate: Date,
    retries: Int = 1) {

    val retryDate = new Date(delay(retries))

    def next = Retry (
      startDate,
      retries + 1)

    def delay(r: Int) = startDate.getTime + scala.math.round(
        60000 * (30 - 29 / scala.math.pow(2, (r * 0.1)))).toLong
  }

  case class Status(
    isActive: Boolean,
    retryInfo: Option[Retry]) {

    require(isActive || retryInfo.isDefined)
    require(!isActive || retryInfo.isEmpty)
  }

}

class CircuitBreaker {
  import CircuitBreaker._

  protected val status =
    scala.collection.mutable.Map[Any,Status]()

  def isActive(any: Any) = {
    val statusOpt = status.get(any)
    if (statusOpt.isDefined) {
      val current = statusOpt.get
      if (current.retryInfo.isDefined) {
        if (current.retryInfo.get.retryDate.getTime <
          (new Date).getTime) true else false
      } else {
        current.isActive
      }
    } else {
      updateStatus(any, true)
      true
    }
  }

  def updateStatus(any: Any, newStatus: Boolean) {
    val statusOpt = status.get(any)
    if (newStatus) {
      status(any) = Status(true, None)
    } else if (statusOpt.isEmpty) {
      status(any) = Status(false, Some(Retry(currentTime)))
    } else if (statusOpt.get.retryInfo.nonEmpty) {
      status(any) =
        Status(false, Some(statusOpt.get.retryInfo.get.next))
    }
  }

  protected def currentTime = new Date

  protected def clear { status.clear() }
}
