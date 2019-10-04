package com.colisweb.distances.re.builder
import cats.Monad
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import com.colisweb.distances.re.cache._
import com.colisweb.distances.re.{AdaptError, DistanceApi, Fallback}

object DistanceApiBuilder {

  implicit class DistanceProviderBuilder[F[_]: Monad, E, O](api: DistanceApi[F, E, O]) {

    def fallback[E2](fallbackProvider: DistanceApi[F, E2, O]): DistanceApi[F, E2, O] =
      Fallback(api, fallbackProvider)

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): DistanceApi[F, E, O] =
      Fallback(DistanceFromCache(cache), api)

    def withCache(
        cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): DistanceApi[F, E, O] =
      DistanceWithCache(api, cacheGet, cacheSet)

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): DistanceApi[F, E, O] =
      DistanceWithCache(api, cache, cache)

    def adaptError[E2](adaptation: E => E2): DistanceApi[F, E2, O] =
      AdaptError(api, adaptation)
  }
}
