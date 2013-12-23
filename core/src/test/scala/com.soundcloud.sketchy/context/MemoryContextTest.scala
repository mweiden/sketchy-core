package com.soundcloud.sketchy.context

import org.scalatest.FlatSpec
import org.joda.time.DateMidnight
import com.soundcloud.sketchy.SpecHelper
import org.scala_tools.time.Imports._

/**
 * Memory event context
 */
class MemoryContextTest extends FlatSpec with SpecHelper {
  behavior of "The memory event context"

  val stat = new TestStatistics(1)

  ignore should "append then get statistics" in {
    val context = memoryCtx()
    context.append(1, stat)

    val stats = context.get(1)
    assert(stats.size === 1)
    assert(stats.head === stat)
  }

  ignore should "delete statistics" in {
    val context = memoryCtx()
    context.append(1, stat)
    assert(context.get(1).size === 1)

    context.delete(1, List(stat.key))
    assert(context.get(1).size === 0)
  }

  it should "create a counter" in {
    val context = memoryCtx()
    context.increment(1, 'chickens)

    assert(context.counter(1, 'chickens) === 1)
  }

  it should "increment a counter" in {
    val context = memoryCtx()
    context.increment(1, 'chickens)
    context.increment(1, 'chickens)

    assert(context.counter(1, 'chickens) === 2)
  }

  it should "delete a counter" in {
    val context = memoryCtx()
    context.increment(1, 'chickens)

    context.deleteCounter(1, 'chickens)
    assert(context.counter(1, 'chickens) === 0)
  }

  it should "add counters across multiple buckets" in {
    val context = memoryCtx()

    incrementAllBuckets(context)
    assert(context.counter(1, 'chickens) === 24)
  }

  it should "delete counters across multiple buckets" in {
    val context = memoryCtx()

    incrementAllBuckets(context)
    context.deleteCounter(1, 'chickens)
    assert(context.counter(1, 'chickens) === 0)
  }

  it should "count across a range of buckets" in {
    val context = memoryCtx()

    incrementAllBuckets(context)
    1.to(24).foreach( i =>
      assert(context.counter(
        1,
        'chickens,
        Option(60 * 60 * (i-1) * 1000L + 1L)) === i)
    )
  }

  def incrementAllBuckets(context: MemoryContext[TestStatistics]) {
    1.to(24).foreach( interval => {
        val currTime = (new DateMidnight()).toDateTime.minusMinutes(30).plusHours(interval)
        val count = context.increment(1, 'chickens, currTime.getMillis)
      }
    )
  }

}
