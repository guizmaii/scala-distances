package com.colisweb.distances.cache

import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DistanceAndDuration, Path}

case class DistanceCache[F[_], P <: Path with CacheKey](
    cache: Cache[F, P, DistanceAndDuration],
    api: DistanceApi[F, P]
) extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    cache.caching(path, api.distance(path))
}
