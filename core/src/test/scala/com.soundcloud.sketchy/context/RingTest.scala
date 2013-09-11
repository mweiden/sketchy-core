package com.soundcloud.sketchy.context

import System.{ currentTimeMillis => now }
import com.soundcloud.sketchy.SpecHelper
import org.scalatest.FlatSpec

/**
 * Time Ring. Seriously.
 */
class RingTest extends FlatSpec with SpecHelper {
  behavior of "Ring constructor"

  it should "only allow integral ticks per bucket" in {
    intercept[IllegalArgumentException] { new Ring(11, 7) }
  }

  behavior of "Ring buckets"

  it should "first bucket is 1 at given time 0" in {
    val ring = new Ring(10, 5)
    assert(ring.bucketsFromEpoch(0L) === 1)
  }

  it should "calculate buckets from epoch at given time" in {
    val ring = new Ring(10, 5)
    assert(ring.bucketsFromEpoch(6L) === 4)
  }

  it should "calculate offset from bucket epoch" in {
    val ring = new Ring(10, 5)
    assert(ring.bucket(7L) === (4,1))
  }

 it should "cycle ring to get bucket for observed time" in {
    val ring = new Ring(10, 5)
    assert(ring.bucket(16L) === (4, 0))
  }

  it should "map buckets correctly" in {
    val ring = new Ring(4, 2)
    assert((0L to 4L).map(ring.bucket(_)._1) == List(1, 1, 2, 2, 1))
  }

  behavior of "Ring ordinals"

  it should "return one ordinal per bucket starting at 1" in {
    val ring = new Ring(10, 5)
    assert(ring.allOrdinals == List(1, 2, 3, 4, 5))
  }

  it should "have ordinals starting with current bucket at given time" in {
    val ring = new Ring(10, 5)
    assert(ring.ordinals(4L) == List(3, 2, 1, 5, 4))
  }

  it should "not have off by one if observed time == ticks" in {
    val ring = new Ring(10, 5)
    assert(ring.ordinals(10L) == List(1, 5, 4, 3, 2))
  }

  it should "return limited ordinals going backward from observed time" in {
    val ring = new Ring(10, 5)
    assert(ring.ordinals(4L, 2) == List(3, 2))
  }
}
