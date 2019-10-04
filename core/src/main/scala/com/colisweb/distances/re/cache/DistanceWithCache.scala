package com.colisweb.distances.re.cache
import cats.Monad
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import com.colisweb.distances.re.{DistanceApi, Fallback}

object DistanceWithCache {

  def apply[F[_]: Monad, E, O](
      api: DistanceApi[F, E, O],
      cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
  ): DistanceApi[F, E, O] =
    Fallback(
      DistanceFromCache[F, O](cacheGet),
      DistanceUpdateCache[F, E, O](api, cacheSet)
    )

  def apply[F[_]: Monad, E, O](
      api: DistanceApi[F, E, O],
      cache: Cache[F, Path[O], DistanceAndDuration]
  ): DistanceApi[F, E, O] =
    apply(api, cache, cache)
}
