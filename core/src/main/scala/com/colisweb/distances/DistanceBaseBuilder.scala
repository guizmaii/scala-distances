package com.colisweb.distances

import cats.MonadError
import com.colisweb.distances.cache.{Cache, CacheKey, DistanceCache}
import com.colisweb.distances.model.{DistanceAndDuration, Path}

trait DistanceBuilder[F[_], P <: Path] {
  val api: DistanceApi[F, P]
  protected def builder(api: DistanceApi[F, P]): DistanceBuilder[F, P]

  def build: DistanceApi[F, P] = api

  def fallback(other: DistanceApi[F, P])(implicit F: MonadError[F, Throwable]): DistanceBuilder[F, P] =
    builder(Fallback(api, other))

  def fallback(other: DistanceBuilder[F, P])(implicit F: MonadError[F, Throwable]): DistanceBuilder[F, P] =
    builder(Fallback(api, other.api))
}

case class DistanceBaseBuilder[F[_], P <: Path](api: DistanceApi[F, P]) extends DistanceBuilder[F, P] {
  override protected def builder(api: DistanceApi[F, P]): DistanceBuilder[F, P] = copy(api = api)
}

case class DistanceCacheBuilder[F[_], P <: Path with CacheKey](api: DistanceApi[F, P]) extends DistanceBuilder[F, P] {
  override protected def builder(api: DistanceApi[F, P]): DistanceBuilder[F, P] = copy(api = api)

  def cache(cache: Cache[F, P, DistanceAndDuration]): DistanceCacheBuilder[F, P] =
    DistanceCacheBuilder[F, P](DistanceCache(cache, api))
}

object DistanceBuilder {

  def apply[F[_], P <: Path](api: DistanceApi[F, P]): DistanceBuilder[F, P] =
    DistanceBaseBuilder(api)

  def withCacheKey[F[_], P <: Path with CacheKey](api: DistanceApi[F, P]): DistanceCacheBuilder[F, P] =
    DistanceCacheBuilder(api)
}
