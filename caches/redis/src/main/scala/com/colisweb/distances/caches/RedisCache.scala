package com.colisweb.distances.caches

import cats.Functor
import cats.implicits._
import com.colisweb.distances.caches.RedisCache.KeyBuilder
import com.colisweb.distances.re.cache.{Cache, CacheGet, CacheSet}
import scalacache.Mode
import scalacache.redis.{RedisCache => RedisCacheApi}
import scalacache.serialization.Codec

import scala.concurrent.duration.FiniteDuration

class RedisCacheGet[F[_]: Mode, K: KeyBuilder, V](
    cacheApi: RedisCacheApi[V]
) extends CacheGet[F, K, V] {

  override def get(key: K): F[Option[V]] =
    cacheApi.get[F](KeyBuilder[K].parts(key))
}

class RedisCacheSet[F[_]: Functor: Mode, K: KeyBuilder, V](
    cacheApi: RedisCacheApi[V],
    ttl: Option[FiniteDuration]
) extends CacheSet[F, K, V] {

  override def set(key: K, value: V): F[Unit] =
    cacheApi.put(KeyBuilder[K].parts(key))(value, ttl).map(_ => ())
}

class RedisCache[F[_]: Functor: Mode, K, V](
    cacheGet: CacheGet[F, K, V],
    cacheSet: CacheSet[F, K, V]
) extends Cache[F, K, V] {

  override def get(key: K): F[Option[V]]      = cacheGet.get(key)
  override def set(key: K, value: V): F[Unit] = cacheSet.set(key, value)
}

object RedisCache {

  trait KeyBuilder[K] {
    def parts(key: K): Seq[Any]
  }
  object KeyBuilder {
    def apply[K](implicit KP: KeyBuilder[K]): KeyBuilder[K] = KP
  }
  def keyBuilder[K](builder: K => Seq[Any]): KeyBuilder[K] = builder.apply
  def productKeyBuilder[K <: Product]: KeyBuilder[K]       = _.productIterator.toSeq

  def getOnly[F[_]: Mode, K: KeyBuilder, V: Codec](config: RedisConfiguration): RedisCacheGet[F, K, V] =
    new RedisCacheGet[F, K, V](RedisCacheApi(config.jedisPool))

  def setOnly[F[_]: Functor: Mode, K: KeyBuilder, V: Codec](
      config: RedisConfiguration,
      ttl: Option[FiniteDuration]
  ): RedisCacheSet[F, K, V] =
    new RedisCacheSet[F, K, V](RedisCacheApi(config.jedisPool), ttl)

  def apply[F[_]: Functor: Mode, K: KeyBuilder, V: Codec](
      config: RedisConfiguration,
      ttl: Option[FiniteDuration]
  ): RedisCache[F, K, V] = {
    val innerCache = RedisCacheApi[V](config.jedisPool)
    new RedisCache[F, K, V](new RedisCacheGet(innerCache), new RedisCacheSet(innerCache, ttl))
  }
}
