package com.colisweb.distances

import cats.{Applicative, MonadError}
import com.colisweb.distances.bird.HaversineDistanceApi
import com.colisweb.distances.cache.{Cache, DistanceFromCache, DistanceWithCache}
import com.colisweb.distances.model.{DistanceAndDuration, FixedSpeedTransportation, OriginDestination}

case class Distances[F[_], P](api: DistanceApi[F, P]) {

  def fallback(other: DistanceApi[F, P])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other))

  def fallback(other: Distances[F, P])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other.api))

  def caching(cache: Cache[F, P, DistanceAndDuration]): Distances[F, P] =
    copy(api = DistanceWithCache(cache, api))
}

object Distances {

  def haversine[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]: Distances[F, P] =
    from(new HaversineDistanceApi[F, P])

  implicit def from[F[_], P](api: DistanceApi[F, P]): Distances[F, P] = Distances(api)

  implicit def fromCache[F[_], P](
      cache: Cache[F, P, DistanceAndDuration]
  )(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    from(DistanceFromCache(cache))
}
