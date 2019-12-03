package com.colisweb.distances.builder

import cats.{Monad, Parallel}
import com.colisweb.distances
import com.colisweb.distances.cache.{Cache, DistanceCache, DistanceCacheBatch}
import com.colisweb.distances.model.{DistanceAndDuration, Path}
import com.colisweb.distances._

object Builders {
  object Base       extends BaseBuilders
  object Sequential extends BaseBuilders with SequentialBuilders
  object Parallel   extends BaseBuilders with ParallelBuilders
}

trait BaseBuilders {

  implicit class Builder[F[_]: Monad, E, R](builder: Distances.Builder[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, E2, R]): Distances.Builder[F, E2, R] =
      Fallback(builder, fallbackBuilder)

    def fromCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      FallbackOption(DistanceCache.from(cache), builder)

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.Builder[F, E, R] =
      distances.FallbackOption(
        DistanceCache.from(cache),
        DistanceCache.update(builder, cache)
      )

    def adaptError[E2](adaptation: E => E2): Distances.Builder[F, E2, R] =
      AdaptError(builder)(adaptation)

    def optional: Distances.BuilderOption[F, R] =
      Optional(builder)
  }

  implicit class BatchBuilder[F[_]: Monad, E, R](builder: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.BuilderBatch[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(builder, fallbackBuilder)

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

  implicit class BatchOptionalBuilder[F[_]: Monad, R](builder: Distances.BuilderBatchOption[F, R]) {

    def nonOptional[E](error: => E): Distances.BuilderBatch[F, E, R] =
      NonOptionalBatch(builder)(error)

    def fallback[E](fallbackBuilder: Distances.BuilderBatch[F, E, R]): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(builder, fallbackBuilder)
  }
}

trait SequentialBuilders {

  implicit class BatchSequentialBuilder[F[_]: Monad, E, R](builder: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(builder, BatchSingle.sequential(fallbackBuilder))

    def fromCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(BatchSingleOptional.sequential(DistanceCache.from(cache)), builder)

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      distances.FallbackBatchOption(
        BatchSingleOptional.sequential[F, R](DistanceCache.from[F, R](cache)),
        DistanceCacheBatch.updateSequential[F, E, R](builder, cache)
      )
  }

  implicit class BatchSequentialSingleBuilder[F[_]: Monad, E, R](builder: Distances.Builder[F, E, R]) {

    def batched: Distances.BuilderBatch[F, E, R] =
      BatchSingle.sequential(builder)
  }

  implicit class OptionalBatchSequentialSingleBuilder[F[_]: Monad, E, R](builder: Distances.BuilderOption[F, R]) {

    def batched: Distances.BuilderBatchOption[F, R] =
      BatchSingleOptional.sequential(builder)
  }
}

trait ParallelBuilders {

  implicit class BatchParallelBuilder[F[_]: Monad: Parallel, E, R](builder: Distances.BuilderBatch[F, E, R]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, E2, R]): Distances.BuilderBatch[F, E2, R] =
      FallbackBatch(builder, BatchSingle.parallel(fallbackBuilder))

    def fromCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      FallbackBatchOption(BatchSingleOptional.parallel(DistanceCache.from(cache)), builder)

    def withCache(cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderBatch[F, E, R] =
      distances.FallbackBatchOption(
        BatchSingleOptional.parallel[F, R](DistanceCache.from[F, R](cache)),
        DistanceCacheBatch.updateParallel[F, E, R](builder, cache)
      )
  }

  implicit class BatchParallelSingleBuilder[F[_]: Monad: Parallel, E, R](builder: Distances.Builder[F, E, R]) {

    def batched: Distances.BuilderBatch[F, E, R] =
      BatchSingle.parallel(builder)
  }

  implicit class OptionalBatchParallelSingleBuilder[F[_]: Monad: Parallel, E, R](
      builder: Distances.BuilderOption[F, R]
  ) {

    def batched: Distances.BuilderBatchOption[F, R] =
      BatchSingleOptional.parallel(builder)
  }
}
