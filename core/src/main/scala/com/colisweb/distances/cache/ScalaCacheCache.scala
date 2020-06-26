package com.colisweb.distances.cache

import scalacache.{CacheAlg, Flags, Mode}

import scala.concurrent.duration.FiniteDuration

class ScalaCacheCache[F[_]: Mode, K <: CacheKey, V](cache: CacheAlg[V], flags: Flags, ttl: Option[FiniteDuration])
    extends Cache[F, K, V] {
  private implicit val implicitFlags: Flags = flags

  override def get(key: K): F[Option[V]]             = cache.get(key.parts)
  override def put(key: K, value: V): F[Any]         = cache.put(key.parts)(value, ttl)
  override def caching(key: K, value: => F[V]): F[V] = cache.cachingF(key.parts)(ttl)(value)
}

object ScalaCacheCache {

  def apply[F[_]: Mode, K <: CacheKey, V](
      cache: CacheAlg[V],
      flags: Flags,
      ttl: Option[FiniteDuration]
  ): ScalaCacheCache[F, K, V] =
    new ScalaCacheCache(cache, flags, ttl)
}
