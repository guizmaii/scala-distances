package com.colisweb.distances.re.builder
import cats.{Monad, Parallel}
import com.colisweb.distances.re._
import com.colisweb.distances.re.cache._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

object DistanceApiBuilder {

  implicit class Builder[F[_]: Monad, E, R](builder: Distances.Builder[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, E2, R]): Distances.Builder[F, E2, R] =
      Fallback(builder, fallbackBuilder)

    def fromCache(cache: CacheGet[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      Fallback(DistanceCache.from(cache), builder)

    def withCache(
        cacheGet: CacheGet[F, Path[R], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[R], DistanceAndDuration]
    ): Distances.Builder[F, E, R] =
      Fallback(
        DistanceCache.from(cacheGet),
        DistanceCache.update(builder, cacheSet)
      )

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      withCache(cache, cache)

    def adaptError[E2](adaptation: E => E2): Distances.Builder[F, E2, R] =
      AdaptError.apply(builder)(adaptation)
  }

  implicit class BatchBuilder[F[_]: Monad, E, O](api: Distances.BuilderBatch[F, E, O]) {

    def fallback[E2](fallbackApi: Distances.BuilderBatch[F, E2, O]): Distances.BuilderBatch[F, E2, O] =
      FallbackBatch(api, fallbackApi)

    def adaptError[E2](adaptation: E => E2): Distances.BuilderBatch[F, E2, O] =
      AdaptError(api, adaptation)
  }

  implicit class BatchSequentialBuilder[F[_]: Monad: Parallel, E, O](api: Distances.BuilderBatch[F, E, O]) {

    def fallback[E2](fallbackApi: Distances.Builder[F, E2, O]): Distances.BuilderBatch[F, E2, O] =
      FallbackBatch(api, BatchSingle.sequential(fallbackApi))

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): Distances.BuilderBatch[F, E, O] =
      FallbackBatch(BatchSingleOptional.sequential(DistanceCache.from(cache)), api)

    def withCache(
      cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): Distances.BuilderBatch[F, E, O] =
      FallbackBatch(
        BatchSingleOptional.sequential[F, O](DistanceCache.from[F, O](cacheGet)),
        DistanceCacheBatch.sequential[F, E, O](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): Distances.BuilderBatch[F, E, O] =
      withCache(cache, cache)
  }

  implicit class BatchParallelBuilder[F[_]: Monad: Parallel, E, O](api: Distances.BuilderBatch[F, E, O]) {

    def fallback[E2](fallbackApi: Distances.Builder[F, E2, O]): Distances.BuilderBatch[F, E2, O] =
      FallbackBatch(api, BatchSingle.parallel(fallbackApi))

    def fromCache(cache: CacheGet[F, Path[O], DistanceAndDuration]): Distances.BuilderBatch[F, E, O] =
      FallbackBatch(BatchSingleOptional.parallel(DistanceCache.from(cache)), api)

    def withCache(
        cacheGet: CacheGet[F, Path[O], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
    ): Distances.BuilderBatch[F, E, O] =
      FallbackBatch(
        BatchSingleOptional.parallel[F, O](DistanceCache.from[F, O](cacheGet)),
        DistanceCacheBatch.parallel[F, E, O](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[O], DistanceAndDuration]): Distances.BuilderBatch[F, E, O] =
      withCache(cache, cache)
  }
}
