package com.soundcloud.sketchy.util

import net.spy.memcached._

/**
 * Pool of Memcached servers with consistent hashing
 */
object MemcachedConnectionPool {
  def get(addresses: String): MemcachedClient = {
    new MemcachedClient(
      new KetamaConnectionFactory(),
      AddrUtil.getAddresses(addresses))
  }
}

/**
 * Basic Spy Memcached transcoder with string transcoding
 */
trait SpyTranscoder {
  def asyncDecode(data: CachedData) = false
  def getMaxSize() = CachedData.MAX_SIZE
  val charset = "UTF-8"

  def cachedData(content: String): CachedData =
    new CachedData(0, content.getBytes(charset), getMaxSize())
  def cachedString(data: CachedData): String =
    new String(data.getData(), charset)
}
