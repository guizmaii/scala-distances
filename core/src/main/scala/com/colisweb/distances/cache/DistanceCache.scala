package com.colisweb.distances.cache

import cats.data.Kleisli
import cats.implicits._
import cats.{Monad, Parallel}
import com.colisweb.distances.Distances
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object DistanceCache {

  def from[F[_], R](cache: Cache[F, Path[R], DistanceAndDuration]): Distances.BuilderOption[F, R] =
    Kleisli(cache.get)

  def update[F[_]: Monad, E, R](
      api: Distances.Builder[F, E, R],
      cache: Cache[F, Path[R], DistanceAndDuration]
  ): Distances.Builder[F, E, R] =
    Kleisli(
      path =>
        api(path).flatMap {
          case Right(value)   => cache.put(path, value).map(_ => Right(value))
          case left @ Left(_) => Monad[F].pure(left)
        }
    )
}

object DistanceCacheBatch {

  def updateSequential[F[_]: Monad, E, R](
      api: Distances.BuilderBatch[F, E, R],
      cache: Cache[F, Path[R], DistanceAndDuration]
  ): Distances.BuilderBatch[F, E, R] =
    api.andThen { results =>
      results
        .collect { case (path, Right(value)) => path -> value }
        .toList
        .traverse { case (path, value) => cache.put(path, value) }
        .map(_ => results)
    }

  def updateParallel[F[_]: Monad: Parallel, E, O](
      api: Distances.BuilderBatch[F, E, O],
      cache: Cache[F, Path[O], DistanceAndDuration]
  ): Distances.BuilderBatch[F, E, O] =
    api.andThen { results =>
      results
        .collect { case (path, Right(value)) => path -> value }
        .toList
        .parTraverse { case (path, value) => cache.put(path, value) }
        .map(_ => results)
    }
}
