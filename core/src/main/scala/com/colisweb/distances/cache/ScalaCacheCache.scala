package com.colisweb.distances.cache

import ScalaCacheCache.Key
import scalacache.{CacheAlg, Flags, Mode}

import scala.concurrent.duration.FiniteDuration

class ScalaCacheCache[F[_]: Mode, K: Key, V](cache: CacheAlg[V], flags: Flags, ttl: Option[FiniteDuration])
    extends Cache[F, K, V] {
  import Key.KeyOps
  private implicit val implicitFlags: Flags = flags

  override def get(key: K): F[Option[V]]     = cache.get(key.parts)
  override def put(key: K, value: V): F[Any] = cache.put(key.parts)(value, ttl)
}

object ScalaCacheCache {

  trait Key[K] {
    def parts(key: K): Seq[Any]
  }
  object Key {
    def apply[K](implicit KP: Key[K]): Key[K] = KP
    implicit class KeyOps[K: Key](key: K) {
      def parts: Seq[Any] = Key[K].parts(key)
    }

    def fromFunction[K](builder: K => Seq[Any]): Key[K] = builder.apply
    def forProduct[K <: Product]: Key[K]                = _.productIterator.toSeq
  }

  def apply[F[_]: Mode, K: Key, V](
      cache: CacheAlg[V],
      flags: Flags,
      ttl: Option[FiniteDuration]
  ): ScalaCacheCache[F, K, V] =
    new ScalaCacheCache(cache, flags, ttl)
}
