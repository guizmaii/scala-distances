package com.colisweb.distances.cache

import cats.Monad
import cats.data.Kleisli
import cats.implicits._
import com.colisweb.distances.Distances
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object DistanceCache {

  def from[F[_], P <: Path with CacheKey](cache: Cache[F, P, DistanceAndDuration]): Distances.BuilderOption[F, P] =
    Kleisli(cache.get)

  def caching[F[_]: Monad, P <: Path with CacheKey, E](
      builder: Distances.Builder[F, P, E],
      cache: Cache[F, P, DistanceAndDuration]
  ): Distances.Builder[F, P, E] =
    Kleisli(
      path =>
        cache.get(path).flatMap {
          case Some(value) => Monad[F].pure(Right(value))
          case None =>
            builder(path).flatMap {
              case Right(value)   => cache.put(path, value).map(_ => Right(value))
              case left @ Left(_) => Monad[F].pure(left)
            }
        }
    )
}

object DistanceCacheBatch {

//  def updateSequential[F[_]: Monad, E, R](
//      api: Distances.BuilderBatch[F, E, R],
//      cache: Cache[F, Path[R], DistanceAndDuration]
//  ): Distances.BuilderBatch[F, E, R] =
//    api.andThen { results =>
//      results
//        .collect { case (path, Right(value)) => path -> value }
//        .toList
//        .traverse { case (path, value) => cache.put(path, value) }
//        .map(_ => results)
//    }
//
//  def updateParallel[F[_]: Monad: Parallel, E, O](
//      api: Distances.BuilderBatch[F, E, O],
//      cache: Cache[F, Path[O], DistanceAndDuration]
//  ): Distances.BuilderBatch[F, E, O] =
//    api.andThen { results =>
//      results
//        .collect { case (path, Right(value)) => path -> value }
//        .toList
//        .parTraverse { case (path, value) => cache.put(path, value) }
//        .map(_ => results)
//    }
}
