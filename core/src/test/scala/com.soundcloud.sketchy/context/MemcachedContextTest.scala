package com.soundcloud.sketchy.context

import org.scalatest.FlatSpec
import org.joda.time.DateMidnight
import org.scala_tools.time.Imports._
import com.soundcloud.sketchy.events.UserEventKey
import com.soundcloud.sketchy.SpecHelper

/**
 * Memcached event context
 */
class MemcachedContextTest extends FlatSpec with SpecHelper {
  behavior of "The memcached event context statistics"

  val stat = TestStatistics(1)

  it should "append then get statistics" in {
    withMemcached { memory =>
      val context = memcachedStatisticsCtx(memory)
      context.append(1, stat)

      val stats = context.get(1)
      assert(stats.size === 1)
      assert(stats.head === stat)
    }
  }

  it should "delete statistics" in {
    withMemcached { memory =>
      val context = memcachedStatisticsCtx(memory)
      context.append(1, stat)
      assert(context.get(1).size === 1)

      context.delete(1, List(stat.key))
      assert(context.get(1).size === 0)
    }
  }

  it should "expire the statistics list after the User TTL is up" in {
    withMemcached { memory =>
      val context = memcachedStatisticsCtx(client = memory, userTtl = 1)
      context.append(2, stat)
      Thread.sleep(2000)
      assert(context.get(2) === Nil)
    }
  }

  // This test will not pass because jmemcached's do not include touch
  // functionality for the ASCII protocol.
  ignore should "renew the statistics list on append" in {
    withMemcached { memory =>
      val context = memcachedStatisticsCtx(client = memory, userTtl = 1)
      context.append(1, stat)
      Thread.sleep(750)
      context.append(1, stat)
      Thread.sleep(750)
      assert(context.get(1).length === 1)
    }
  }


  behavior of "The memcached event context counters"

  it should "create a counter" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      context.increment(1, 'chickens)
      assert(context.counter(1, 'chickens) === 1)
    }
  }

  it should "increment a counter" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      context.increment(1, 'chickens)
      context.increment(1, 'chickens)
      assert(context.counter(1, 'chickens) === 2)
    }
  }

  it should "delete a counter" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      context.increment(1, 'chickens)
      context.deleteCounter(1, 'chickens)
      assert(context.counter(1, 'chickens) === 0)
    }
  }

  it should "generate unique throttle keys given different namespaces" in {
    withMemcached { memory =>
      val context1 = memcachedCtx(memory, "cx")
      val context2 = memcachedCtx(memory, "cc")

      context1.increment(1, 'chickens)
      context2.increment(1, 'chickens)
      assert(context1.counter(1, 'chickens) === 1)
      assert(context2.counter(1, 'chickens) === 1)
    }
  }

  it should "add counters across multiple buckets" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      incrementAllBuckets(context)
      assert(context.counter(1, 'chickens) === 24)
    }
  }

  it should "delete counters across multiple buckets" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      incrementAllBuckets(context)
      context.deleteCounter(1, 'chickens)
      assert(context.counter(1, 'chickens) === 0)
    }
  }

  it should "count from observed time" in {
    withMemcached { memory =>
      val context = memcachedCtx(memory)

      incrementAllBuckets(context)

      1.to(24).foreach( i =>
        assert(context.counter(1, 'chickens, Option(60 * 60 * (i-1) * 1000L)) === i)
      )
    }
  }

  def incrementAllBuckets(context: MemcachedTestContext) {
    1.to(24).foreach( interval => {
      val currTime = (new DateMidnight()).toDateTime.minusMinutes(30).plusHours(interval)
      val count = context.increment(1, 'chickens, currTime.getMillis)
      }
    )
  }

}

