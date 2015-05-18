package com.soundcloud.sketchy.context

import System.{ currentTimeMillis => now }

import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.{ CachedData, MemcachedClient, CASMutator, CASMutation }

import org.joda.time.DateMidnight
import org.scala_tools.time.Imports._
import scala.collection.JavaConversions._
import scala.collection.mutable

import com.soundcloud.sketchy.monitoring.Instrumented
import com.soundcloud.sketchy.events.UserEventKey
import com.soundcloud.sketchy.util.SpyTranscoder

/**
 * A rolling window of statistics, keyed by user. There is also set of unkeyed
 * methods for manipulation of a single global statistic window.
 */
sealed trait Context[T <: Statistics] {

  /**
   * Get statistics seq for a user
   *
   * @param id the user identifier
   * @return a rolling window of Statistic objects
   */
  def get(id: Long): Seq[T]

  /**
   * Multiget the statistics for a list of users
   *
   * @param ids the user identifiers
   * @return a sigle list of all Statistic objects
   */
  def get(ids: List[Long]): Seq[T] =
    ids.foldLeft[Seq[T]](Seq())((s, id) => s ++ get(id))

  /**
   * Appends a statistic to a rolling window for a user
   *
   * @param id the user identifier
   * @param a single statistic to add to user's window
   */
  def append(id: Long, statistics: T)

  /**
   * Deletes given statistics from the user's rolling window
   *
   * @param id the user identifier
   * @param items the statistics to delete from given user's window
   */
  def delete(id: Long, items: Seq[UserEventKey])

  /**
   * Get a user's window, partitioned by statistic type
   *
   * @param id the user identifier
   */
  def getPartitioned(id: Long): Map[String, Seq[T]] =
    get(id).groupBy(_.key.kind)

  /**
   * Multiget statistics for many users, partitioned by statistic type
   *
   * @param ids the list of user identifiers
   * @return a single map with all (id, statistic) keyed by type
   */
  def getPartitioned(ids: List[Long]): Map[String, Seq[(Long, T)]] =
    ids
      .foldLeft[Seq[(Long, T)]](Seq())((s, id) => s ++ get(id).map((id, _)))
      .groupBy(_._2.key.kind)

  /**
   * Atomically increment a user counter for a given key
   *
   * @param id the user identifier
   * @param name the counter name
   * @param at the observed time
   * @return the incremented counter value
   */
  def increment(id: Long, name: Symbol, at: Long = now): Long

  /**
   * Get the user counter for a given name. This is a rolling (decaying)
   * counter, and is parameterized by the ring configured for this context.
   *
   * Example:
   *
   *   Given
   *     - a ring which cycles in one day and has 24 buckets
   *     - a context with counter 'foo incremented to 1 for user 1
   *
   *   Then
   *     - the counter value will be 1 after 23 hours
   *     - the counter value will be 0 after 24 hours
   *
   * The counter can also be constrained to return only events counted in the
   * last n seconds. This is controlled via timeLimit.
   *
   * @param id the user identifier
   * @param name the counter name
   * @param timeLimit limit to counts seen in this number of seconds
   * @return the current counter value
   */
  def counter(id: Long, name: Symbol, timeLimit: Option[Long] = None): Long

  /**
   * Deletes a single user counter
   *
   * @param id the user identifier
   * @param name the counter name
   */
  def deleteCounter(id: Long, name: Symbol)

  // GLOBAL

  /**
   * Get the gobal statistics
   *
   * @param id the user identifier
   * @return a rolling window of statistic objects
   */
  def get(): Seq[T] = get(0)

 /**
   * Appends a single statistic to a global rolling window
   *
   * @param id the user identifier
   * @param a single statistic to add to user's window
   */
  def append(statistics: T) { append(0, statistics) }

  /**
   * Deletes the statistics from the global rolling window
   *
   * @param id the user identifier
   * @param a rolling window of statistic objects
   */
  def delete(items: Seq[UserEventKey]) { delete(0, items) }

  /**
   * Get statistics from the global window, partitioned by type
   */
  def getPartitioned(): Map[String, Seq[T]] =
    getPartitioned(0)

}

/**
 * Context options
 *
 * @param ttl time to live for a single statistic in the context
 * @param slack number of entries over the secondsPerBucket before compaction
 * @param fragLimit proportion of missing entries before compaction {0,1}
 * @param blockingDelete wait until all deletes are done; useful for testing
 */
case class ContextCfg(
  ttl: Int = 24 * 60 * 60,
  numBuckets: Int = 96,
  slack: Int = 200,
  fragLimit: Double = 0.5,
  blockingDelete: Boolean = false,
  userTtl: Int = ContextCfg.MaxTTL) {

  val ring = new Ring(ttl * 1000, numBuckets)
}

object ContextCfg {
  val MaxTTL = 30 * 24 * 60 * 60 - 1
}

/**
 * In memory implementation of a temporal context, partitions on id
 */
class MemoryContext[T <: Statistics](cfg: ContextCfg = ContextCfg()) extends Context[T] {
  val windows: mutable.Map[Long, Seq[T]] = mutable.Map()
  val counters: mutable.Map[(Long, Symbol, Int), Long] = mutable.Map()

  def append(id: Long, statistics: T) =
    synchronized {
      windows(id) = (get(id) :+ statistics).take(cfg.ring.secondsPerBucket)
    }

  def get(id: Long): Seq[T] =
    synchronized {
      if (windows.contains(id)) windows(id) else Nil
    }

  def delete(id: Long, items: Seq[UserEventKey]) =
    synchronized {
      if (windows.contains(id)) {
        windows(id) = windows(id).filter(x => !items.contains(x.key))
      }
    }

  def increment(id: Long, name: Symbol, at: Long = now): Long =
    synchronized {
      val (bucket, offset) = cfg.ring.bucket(at)
      val current = counters.get((id, name, bucket)).getOrElse[Long](0)
      counters((id, name, bucket)) = current + 1
      current + 1
    }

  def counter(id: Long, name: Symbol, timeLimit: Option[Long] = None): Long =
    synchronized {
      val toCounters = (n: Int) => counters.get((id, name, n)).getOrElse(0L)
      cfg.ring.ordinals(timeLimit).map(toCounters).sum
    }

  def deleteCounter(id: Long, name: Symbol) {
    synchronized {
      cfg.ring.allOrdinals.map(bucket => counters((id, name, bucket)) = 0)
    }
  }
}

/**
 * A sliding window with a time to live for entries, backed by Memcached
 *
 * Lists are constructed with a two stage fetch as described in
 *
 * @see http://lists.danga.com/pipermail/memcached/2007-July/004578.html
 *
 * =append=
 *
 * New entries are atomically appended to this list with the binary protocol
 * primitive. There is no rotation on append. The CAS primitives would have to
 * be used for atomic delete / append rotation. However this would require
 * every append to send and retrieve the whole list. Instead, compaction and
 * repair of the growing lists is periodically handled by #get.
 *
 * First get the window associated with a user. Here's an example showing a
 * list of junk messages associated with user 1 in the namespace 'j'.
 *
 * @example "j:1", "j:m:120,j:m:121,j:m:122"
 *
 * Next the referenced keys are retrieved with a multiget. Note that some of
 * the referenced keys may have expired. The returned list is finally
 * truncated to the requested secondsPerBucket.
 *
 * If the proportion of expired but referenced keys is too high, or the window
 * size has grown over a maximum threshold, the list is repaired.
 *
 * =delete=
 *
 * No attempt is made to make batch deletion atomic. The keys are individually
 * deleted from memcached.
 *
 * =repair=
 *
 * This is a CAS operation. The list is retrieved, checked for expired entries
 * and old entries are dropped from the head. The new list is saved against
 * the read version and retries the repair in case of conflict.
 *
 * =caveats=
 *
 * Continuously sliding windows for counters cannot be mapped exactly onto
 * memcached. Instead, this implementation provides discretely sliding
 * windows. The actual window that can be looked back into varies between the
 * given bounds:
 *
 *   (numberOfBuckets - 1) * ticksPerBucket < actualWindow < ttl
 *
 * @param memory memcached client. expect to be connected to several instances
 * @param options CacheContext.Options
 */
abstract class CacheContext[T <: Statistics](
  memory: MemcachedClient,
  cfg: ContextCfg) extends Context[T] with Instrumented {

  def metricsNameArray = this.getClass.getName.split('.')
  def metricsTypeName = metricsNameArray(metricsNameArray.length - 1)
  def metricsSubtypeName = Some(metricsNameArray(metricsNameArray.length - 2))

  // implement
  def transcoder: Transcoder[T]
  def namespace: String

  var keyTranscoder = new KeyTranscoder()

  // blocking
  def append(id: Long, stats: T) = {
    // Adds an extra initialization roundtrip for known users. Catching
    // a failed append then adding to the list would require extra race
    // handling.
    val fAdd = memory.add(userKey(id), cfg.userTtl, "")

    // No cas required (0). Block until we know the key exists.
    if (fAdd.get()) {
      meter("list_add", "success")
    } else {
      if (memory.touch(userKey(id), cfg.userTtl).get()) {
        meter("list_renew", "success")
      } else {
        meter("list_renew", "failure")
      }
    }

    // encode the string as ascii bytes
    val asciiBytes = stats.marshalled.getBytes("ascii")

    // fail if there are any unknown characters in the string
    require(!asciiBytes.contains(63))

    // Create the datum with the requested TTL
    val fSet = memory.set(datumKey(stats.key), cfg.ttl, asciiBytes)

    val fAppend = memory.append(0, userKey(id), " " + datumKey(stats.key))

    // Append is currently a blocking operation
    fSet.get()
    fAppend.get()
  }

  // 2-stage read
  def get(id: Long): Seq[T] = {
    val list: List[UserEventKey] = getUserEventKeys(id)
    val found: List[T] = getStatistics(list)

    // repair
    if (shouldRepair(list.size, found.size)) repair(id)

    // found keys ordered and intersected by list; return associated T
    val lookup: Map[UserEventKey, T] = found.map(f => (f.key, f)).toMap
    list.intersect(found.map(_.key)).map(lookup(_)).toSeq
  }

  // Blocks if blockingDelete. Makes no attempt at atomic batch.
  def delete(id: Long, items: Seq[UserEventKey]) = {
    val deletions = items.map { key => memory.delete(datumKey(key)) }
    if (cfg.blockingDelete) deletions.map(_.get())
  }

  // Increment a counter, either for specific bucket or current bucket
  def increment(id: Long, name: Symbol, at: Long = now): Long = {
    val (bucket, offset) = cfg.ring.bucket(at)
    memory.incr(throttleKey(id, name, bucket), 1, 1, cfg.ttl - (offset / 1000))
  }

  // Sum counters, either for range of buckets from current or for all
  def counter(id: Long, name: Symbol, timeLimit: Option[Long] = None): Long = {
    val ordinals = cfg.ring.ordinals(timeLimit)
    val keys = ordinals.map(throttleKey(id, name, _))

    memory.getBulk(keys).values.foldLeft[Long](0)((b, a) => a.toString.toLong + b)
  }

  def deleteCounter(id: Long, name: Symbol) {
    val keys = cfg.ring.allOrdinals.map(throttleKey(id, name, _))
    val deletions = keys.map { key => memory.delete(key) }
    if (cfg.blockingDelete) deletions.map(_.get())
  }

  private def repair(id: Long) = {
    // retried on contention
    val repair = new CASMutation[List[UserEventKey]]() {
      def getNewValue(list: List[UserEventKey]): List[UserEventKey] = {

        // dereference
        val found: List[T] = getStatistics(list)

        // ordered intersect to window size
        list.intersect(found.map(_.key)).takeRight(cfg.ring.secondsPerBucket)
      }
    }

    // empty lists not expected to reach repair
    val initial: List[UserEventKey] = Nil

    // async
    val mutator = new CASMutator[List[UserEventKey]](memory, keyTranscoder)
    mutator.cas(userKey(id), initial, 0, repair)
    meter("repair", "success")
  }

  private def getUserEventKeys(id: Long): List[UserEventKey] = {
    val values = memory.get(userKey(id), keyTranscoder)
    if (values == null) Nil else values
  }

  private def getStatistics(list: List[UserEventKey]): List[T] = {
    val bulk = memory.getBulk(windowKeys(list), transcoder)
    if (bulk == null) Nil else bulk.values.toList
  }
  private def throttleKey(id: Long, name: Symbol, bucket: Int) =
    List(namespace, id, "th", name.name, bucket).mkString(":")

  private def userKey(id: Long): String =
    namespace + ":" + id

  private def datumKey(origin: UserEventKey): String =
    namespace + ":" + origin.marshalled

  private def joinWindow(list: List[UserEventKey]): String =
    windowKeys(list).mkString(" ")

  private def windowKeys(list: List[UserEventKey]): List[String] =
    list.map(datumKey(_))

  private def splitWindow(input: String): List[UserEventKey] =
    *(input).map(_.drop(namespace.size + 1)).map(UserEventKey.unmarshal _).toList

  private def *(input: String) =
    input.trim().split(" ").filterNot(_ == "")

  private def shouldRepair(listed: Int, found: Int): Boolean =
    isFragmented(listed, found) || isOversized(listed, found)

  private def isOversized(listed: Int, found: Int): Boolean =
    listed > (cfg.ring.secondsPerBucket + cfg.slack)

  private def isFragmented(listed: Int, found: Int): Boolean =
    (1.0 - (found + 1.0) / (listed + 1.0)) >= cfg.fragLimit

  class KeyTranscoder extends Transcoder[List[UserEventKey]] with SpyTranscoder {
    def encode(list: List[UserEventKey]): CachedData =
      cachedData(joinWindow(list))
    def decode(data: CachedData): List[UserEventKey] =
      splitWindow(cachedString(data))
  }

  // meters
  private val counter = prometheusCounter("context", List("operation", "status"))
  private def meter(operation: String, status: String) {
    counter.labels(operation, status).inc()
  }
}

class NamedCacheContext[S <: Statistics](
  nameSpace: String,
  mem: MemcachedClient,
  contextCfg: ContextCfg)
  (implicit map: StatisticsParserMappings.ClassFunctionMap[S])
  extends CacheContext[S](mem, contextCfg) {

  val unmarshal = map.func

  var transcoder = new Transcoder[S] with SpyTranscoder {
    def encode(stats: S): CachedData =
      cachedData(stats.marshalled)
    def decode(data: CachedData): S =
      unmarshal(cachedString(data))
  }

  def namespace = nameSpace
}

class CountingContext(
  nameSpace: String,
  mem: MemcachedClient,
  contextCfg: ContextCfg) extends CacheContext[Nothing](mem, contextCfg) {

  var transcoder = new Transcoder[Nothing] with SpyTranscoder {
    def encode(stats: Nothing): CachedData = error("cannot encode Nothing")
    def decode(data: CachedData): Nothing = error("cannot decode Nothing")
    def error(msg: String) = throw new Exception(msg)
  }

  def namespace = nameSpace
}
