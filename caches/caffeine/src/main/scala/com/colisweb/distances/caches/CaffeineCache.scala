package com.colisweb.distances.caches

import com.colisweb.distances.re.cache.ScalaCacheCache.Key
import com.colisweb.distances.re.cache.{Cache, ScalaCacheCache}
import scalacache.{Flags, Mode}

import scala.concurrent.duration.FiniteDuration

object CaffeineCache {

  import scalacache.caffeine.{CaffeineCache => CaffeineScalaCache}

  def apply[F[_]: Mode, K: Key, V](flags: Flags, ttl: Option[FiniteDuration]): Cache[F, K, V] = {
    val caffeine = CaffeineScalaCache.apply[V]
    ScalaCacheCache(caffeine, flags, ttl)
  }
}
