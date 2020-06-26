package com.colisweb.distances.builder

import cats.{Monad, Parallel}
import com.colisweb.distances._
import com.colisweb.distances.cache.{Cache, CacheKey, DistanceCache}
import com.colisweb.distances.model.{DistanceAndDuration, Path}


trait Builders[F[_]] {
  protected implicit val M: Monad[F]

  implicit class Builder[P <: Path, E](builder: Distances.Builder[F, P, E]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, P, E2]): Distances.Builder[F, P, E2] =
      Fallback(builder, fallbackBuilder)

    def adaptError[E2](adaptation: E => E2): Distances.Builder[F, P, E2] =
      AdaptError(builder)(adaptation)

    def optional: Distances.BuilderOption[F, P] =
      Optional(builder)
  }

  implicit class CacheBuilder[P <: Path with CacheKey, E](builder: Distances.Builder[F, P, E]) {

    def fromCache(cache: Cache[F, P, DistanceAndDuration]): Distances.Builder[F, P, E] =
      FallbackOption(DistanceCache.from(cache), builder)

    def withCache(cache: Cache[F, P, DistanceAndDuration]): Distances.Builder[F, P, E] =
      DistanceCache.caching(builder, cache)
  }

  implicit class BatchBuilder[P <: Path, E](builder: Distances.BuilderBatch[F, P, E]) {

    def fallback[E2](fallbackBuilder: Distances.BuilderBatch[F, P, E2]): Distances.BuilderBatch[F, P, E2] =
      FallbackBatch(builder, fallbackBuilder)

    def adaptError[E2](adaptation: E => E2): Distances.BuilderBatch[F, P, E2] =
      AdaptErrorBatch(builder, adaptation)

    def optional: Distances.BuilderBatchOption[F, P] =
      OptionalBatch(builder)
  }

  implicit class OptionalBuilder[P <: Path](builder: Distances.BuilderOption[F, P]) {

    def nonOptional[E](error: => E): Distances.Builder[F, P, E] =
      NonOptional(builder)(error)

    def fallback[E](fallbackBuilder: Distances.Builder[F, P, E]): Distances.Builder[F, P, E] =
      FallbackOption(builder, fallbackBuilder)
  }

  implicit class BatchOptionalBuilder[P <: Path](builder: Distances.BuilderBatchOption[F, P]) {

    def nonOptional[E](error: => E): Distances.BuilderBatch[F, P, E] =
      NonOptionalBatch(builder)(error)

    def fallback[E](fallbackBuilder: Distances.BuilderBatch[F, P, E]): Distances.BuilderBatch[F, P, E] =
      FallbackBatchOption(builder, fallbackBuilder)
  }

  implicit class BatchSequentialBuilder[P <: Path, E](builder: Distances.BuilderBatch[F, P, E]) {

    def fallback[E2](fallbackBuilder: Distances.Builder[F, P, E2]): Distances.BuilderBatch[F, P, E2] =
      FallbackBatch(builder, BatchSingle.sequential(fallbackBuilder))
//
//    def withCache(cache: Cache[F, P, DistanceAndDuration]): Distances.BuilderBatch[F, P, E] =
//      distances.FallbackBatchOption(
//        BatchSingleOptional.sequential[F, R](DistanceCache.from[F, R](cache)),
//        DistanceCacheBatch.updateSequential[F, E, R](builder, cache)
//      )
  }

  implicit class BatchSequentialCacheBuilder[P <: Path with CacheKey, E](
      builder: Distances.BuilderBatch[F, P, E]
  ) {

    def fromCache(cache: Cache[F, P, DistanceAndDuration]): Distances.BuilderBatch[F, P, E] =
      FallbackBatchOption(BatchSingleOptional.sequential(DistanceCache.from(cache)), builder)
  }

  implicit class BatchSequentialSingleBuilder[P <: Path, E](builder: Distances.Builder[F, P, E]) {

    def batched: Distances.BuilderBatch[F, P, E] =
      BatchSingle.sequential(builder)
  }

  implicit class OptionalBatchSequentialSingleBuilder[P <: Path, E](
      builder: Distances.BuilderOption[F, P]
  ) {

    def batched: Distances.BuilderBatchOption[F, P] =
      BatchSingleOptional.sequential(builder)
  }
}

trait ParallelBuilders[F[_]] extends Builders[F] {
  protected implicit val P: Parallel[F]

  implicit class BatchParallelBuilder[P <: Path, E](builder: Distances.BuilderBatch[F, P, E]) {

    def parFallback[E2](fallbackBuilder: Distances.Builder[F, P, E2]): Distances.BuilderBatch[F, P, E2] =
      FallbackBatch(builder, BatchSingle.parallel(fallbackBuilder))

//    def withCache(cache: Cache[F, P, DistanceAndDuration]): Distances.BuilderBatch[F, P, E] =
//      distances.FallbackBatchOption(
//        BatchSingleOptional.parallel[F, R](DistanceCache.from[F, R](cache)),
//        DistanceCacheBatch.updateParallel[F, E, R](builder, cache)
//      )
  }

  implicit class BatchParallelCacheBuilder[P <: Path with CacheKey, E](
      builder: Distances.BuilderBatch[F, P, E]
  ) {

    def parFromCache(cache: Cache[F, P, DistanceAndDuration]): Distances.BuilderBatch[F, P, E] =
      FallbackBatchOption(BatchSingleOptional.parallel(DistanceCache.from(cache)), builder)
  }

  implicit class BatchParallelSingleBuilder[P <: Path, E](builder: Distances.Builder[F, P, E]) {

    def parBatched: Distances.BuilderBatch[F, P, E] =
      BatchSingle.parallel(builder)
  }

  implicit class OptionalBatchParallelSingleBuilder[P <: Path, E](
      builder: Distances.BuilderOption[F, P]
  ) {

    def parBatched: Distances.BuilderBatchOption[F, P] =
      BatchSingleOptional.parallel(builder)
  }
}
