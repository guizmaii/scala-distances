package com.colisweb.distances.re.builder
import cats.{Functor, Monad, Parallel}
import com.colisweb.distances.re._
import com.colisweb.distances.re.cache._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

object Builders {
  object Base       extends BaseBuilders
  object Sequential extends BaseBuilders with SequentialBuilders
  object Parallel   extends BaseBuilders with ParallelBuilders
}

trait BaseBuilders {

  implicit class Builder[F[_]: Monad, E, R](builder: Distances.Builder[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, E2, R]): Distances.Builder[F, E2, R] =
      Fallback(builder, fallbackBuilder)

    def fromCache(cache: CacheGet[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      FallbackOption(DistanceCache.from(cache), builder)

    def withCache(
        cacheGet: CacheGet[F, Path[R], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[R], DistanceAndDuration]
    ): Distances.Builder[F, E, R] =
      FallbackOption(
        DistanceCache.from(cacheGet),
        DistanceCache.update(builder, cacheSet)
      )

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      withCache(cache, cache)

    def adaptError[E2](adaptation: E => E2): Distances.Builder[F, E2, R] =
      AdaptError(builder)(adaptation)

    def optional: Distances.BuilderOption[F, R] =
      Optional(builder)
  }

  implicit class BatchBuilder[F[_]: Monad, E, R](builder: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackApi: Distances.BuilderBatch[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(builder, fallbackApi)

    def adaptError[E2](adaptation: E => E2): Distances.BuilderBatch[F, E2, R] =
      AdaptErrorBatch(builder, adaptation)

    def optional: Distances.BuilderBatchOption[F, R] =
      OptionalBatch(builder)
  }

  implicit class OptionalBuilder[F[_]: Monad, R](builder: Distances.BuilderOption[F, R]) {

    def nonOptional[E](error: => E): Distances.Builder[F, E, R] =
      NonOptional(builder)(error)

    def fallback[E](fallbackBuilder: Distances.Builder[F, E, R]): Distances.Builder[F, E, R] =
      FallbackOption(builder, fallbackBuilder)
  }

  implicit class BatchOptionalBuilder[F[_]: Functor, R](buidler: Distances.BuilderBatchOption[F, R]) {

    def nonOptional[E](error: => E): Distances.BuilderBatch[F, E, R] =
      NonOptionalBatch(buidler)(error)
  }
}

trait SequentialBuilders {

  implicit class BatchSequentialBuilder[F[_]: Monad: Parallel, E, R](api: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackApi: Distances.Builder[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(api, BatchSingle.sequential(fallbackApi))

    def fromCache(cache: CacheGet[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(BatchSingleOptional.sequential(DistanceCache.from(cache)), api)

    def withCache(
        cacheGet: CacheGet[F, Path[R], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[R], DistanceAndDuration]
    ): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(
        BatchSingleOptional.sequential[F, R](DistanceCache.from[F, R](cacheGet)),
        DistanceCacheBatch.sequential[F, E, R](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      withCache(cache, cache)
  }
}

trait ParallelBuilders {

  implicit class BatchParallelBuilder[F[_]: Monad: Parallel, E, R](api: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackApi: Distances.Builder[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(api, BatchSingle.parallel(fallbackApi))

    def fromCache(cache: CacheGet[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(BatchSingleOptional.parallel(DistanceCache.from(cache)), api)

    def withCache(
        cacheGet: CacheGet[F, Path[R], DistanceAndDuration],
        cacheSet: CacheSet[F, Path[R], DistanceAndDuration]
    ): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(
        BatchSingleOptional.parallel[F, R](DistanceCache.from[F, R](cacheGet)),
        DistanceCacheBatch.parallel[F, E, R](api, cacheSet)
      )

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      withCache(cache, cache)
  }
}
