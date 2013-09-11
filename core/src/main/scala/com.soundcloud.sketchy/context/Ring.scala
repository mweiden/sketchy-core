package com.soundcloud.sketchy.context

import System.{ currentTimeMillis => now }

/**
 * Number of millisecond ticks per unit
 */
object Ring {
  val Second = 1000
  val Hour = 60 * 60 * Second
  val Day  = 24 * Hour
  val Week = 7 * Day
}

/**
 * A ring configuration, used to convert a time into a bucket ordinal
 *
 * Consider the ring to be like an analog clockface, except that it has
 *
 * @param ticks a tick is a millisecond; total number of ticks in this ring
 * @param buckets number of buckets to be distributed over
 */
class Ring(val ticks: Long = Ring.Day, val buckets: Int = 96) {

  // should have integral ticks per bucket
  require(ticks % buckets == 0)

  /**
   * List of all bucket ordinals in this ring
   */
  val allOrdinals: List[Int] = (1 to buckets).toList

  /**
   * Number of buckets started in the ring at the observed time.
   *
   * @param at the observed time
   * @return current bucket ordinal in ring for the given time, offset from start
   */
  def bucket(at: Long = now): (Int, Int) =
    (bucketsFromEpoch(at % ticks), (at % millisPerBucket).toInt)

  /**
   * @param at the observed time
   * @return total number of buckets started since UNIX epoch
   */
  def bucketsFromEpoch(at: Long): Int = (at / (ticks / buckets)).toInt + 1

  /**
   * List of bucket ordinals ordered backwards on ring from observed time
   *
   * @param at the observed time
   * @return bucket ordinals as a list
   */
  def ordinals(at: Long): List[Int] = {
    val (pivot, offset) = bucket(at)
    val left  = allOrdinals.slice(0, pivot).reverse
    val right = allOrdinals.slice(pivot, buckets).reverse

    left ++ right
  }

  /**
   * List of bucket ordinals going back `tail` buckets from observed time
   *
   * @param at the observed time
   * @param tail number of buckets going back from observed time to include
   * @return bucket ordinals as a list
   */
  def ordinals(at: Long, tail: Int): List[Int] = ordinals(at).take(tail)

  /**
   * List of bucket ordinals going back `timeLimit` seconds from observed time
   *
   * @param timeLimit only count events that happened within timeLimit before
   *        observed time
   * @param at the observed time
   * @return bucket ordinals as a list
   */
  def ordinals(timeLimit: Option[Long], at: Long = now): List[Int] = {
    timeLimit match {
      case Some(timeLimit) => {
        val (tail, offset) = bucket(timeLimit)
        ordinals(at, tail)
      }
      case None => ordinals(at)
    }
  }

  val secondsPerBucket = ((ticks / 1000) / buckets).toInt
  val millisPerBucket = (ticks / buckets).toInt
}
