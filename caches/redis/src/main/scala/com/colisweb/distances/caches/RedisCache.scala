package com.colisweb.distances.caches

import com.colisweb.distances.cache.ScalaCacheCache
import com.colisweb.distances.cache.ScalaCacheCache.Key
import scalacache.serialization.Codec
import scalacache.{Flags, Mode}

import scala.concurrent.duration.FiniteDuration

object RedisCache {
  import scalacache.redis.{RedisCache => RedisScalaCache}

  def apply[F[_]: Mode, K: Key, V: Codec](
      config: RedisConfiguration,
      flags: Flags,
      ttl: Option[FiniteDuration]
  ): ScalaCacheCache[F, K, V] = {
    val redis = RedisScalaCache[V](config.jedisPool)
    ScalaCacheCache(redis, flags, ttl)
  }
}
