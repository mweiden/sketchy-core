package com.soundcloud.sketchy.context

import net.spy.memcached.{ CachedData, MemcachedClient, CASMutator, CASMutation }
import net.spy.memcached.transcoders.Transcoder

import com.soundcloud.sketchy.util.SpyTranscoder

class MemcachedTestContext(
  mem: MemcachedClient,
  contextCfg: ContextCfg,
  nameSpace: String) extends CacheContext[Nothing](mem, contextCfg) {

  var transcoder = new Transcoder[Nothing] with SpyTranscoder {
    def encode(stats: Nothing): CachedData = error("cannot encode Nothing")
    def decode(data: CachedData): Nothing = error("cannot decode Nothing")
    def error(msg: String) = throw new Exception(msg)
  }

  def namespace = nameSpace
}

class MemcachedTestStatisticsContext(
  mem: MemcachedClient,
  contextCfg: ContextCfg,
  nameSpace: String) extends CacheContext[TestStatistics](mem, contextCfg) {

  var transcoder = new Transcoder[TestStatistics] with SpyTranscoder {
    def encode(stats: TestStatistics): CachedData =
      cachedData(stats.marshalled)
    def decode(data: CachedData): TestStatistics =
      TestStatistics.unmarshal(cachedString(data))
    def error(msg: String) = throw new Exception(msg)
  }

  def namespace = nameSpace
}
