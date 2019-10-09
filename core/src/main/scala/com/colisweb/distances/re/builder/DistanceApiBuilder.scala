package com.colisweb.distances.re.builder
import cats.{Monad, Parallel}
import com.colisweb.distances.re._
import com.colisweb.distances.re.cache._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

object DistanceApiBuilder {

  implicit class Builder[F[_]: Monad, E, O](api: DistanceApi[F, E, O]) {

    def fallback[E2](fallbackApi: DistanceApi[F, E2, O]): DistanceApi[F, E2, O] =
      Fallback(api, fallbackApi)

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): DistanceApi[F, E, O] =
      Fallback(DistanceFromCache(cache), api)

    def withCache(
        cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): DistanceApi[F, E, O] =
      Fallback(
        DistanceFromCache(cacheGet),
        DistanceUpdateCache(api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): DistanceApi[F, E, O] =
      withCache(cache, cache)

    def adaptError[E2](adaptation: E => E2): DistanceApi[F, E2, O] =
      AdaptError(api, adaptation)
  }

  implicit class BatchBuilder[F[_]: Monad, E, O](api: DistanceBatchApi[F, E, O]) {

    def fallback[E2](fallbackApi: DistanceBatchApi[F, E2, O]): DistanceBatchApi[F, E2, O] =
      FallbackBatch(api, fallbackApi)

    def fallbackSingle[E2](fallbackApi: DistanceApi[F, E2, O]): DistanceBatchApi[F, E2, O] =
      FallbackBatch(api, BatchSingle.sequential(fallbackApi))

    def adaptError[E2](adaptation: E => E2): DistanceBatchApi[F, E2, O] =
      AdaptError(api, adaptation)

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): DistanceBatchApi[F, E, O] =
      FallbackBatch(BatchSingleOptional.sequential(DistanceFromCache(cache)), api)

    def withCache(
        cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): DistanceBatchApi[F, E, O] =
      FallbackBatch(
        BatchSingleOptional.sequential[F, O](DistanceFromCache[F, O](cacheGet)),
        DistanceUpdateCacheBatch.sequential[F, E, O](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): DistanceBatchApi[F, E, O] =
      withCache(cache, cache)
  }

  implicit class BatchParallelBuilder[F[_]: Monad: Parallel, E, O](api: DistanceBatchApi[F, E, O]) {

    def fallbackSingle[E2](fallbackApi: DistanceApi[F, E2, O]): DistanceBatchApi[F, E2, O] =
      FallbackBatch(api, BatchSingle.parallel(fallbackApi))

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): DistanceBatchApi[F, E, O] =
      FallbackBatch(BatchSingleOptional.parallel(DistanceFromCache(cache)), api)

    def withCache(
        cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): DistanceBatchApi[F, E, O] =
      FallbackBatch(
        BatchSingleOptional.parallel[F, O](DistanceFromCache[F, O](cacheGet)),
        DistanceUpdateCacheBatch.parallel[F, E, O](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): DistanceBatchApi[F, E, O] =
      withCache(cache, cache)
  }
}
