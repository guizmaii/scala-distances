package com.colisweb.distances.cache

import cats.data.Kleisli
import cats.implicits._
import cats.{Monad, MonadError}
import com.colisweb.distances.model.{DistanceAndDuration, DistanceError, Path}
import com.colisweb.distances.{DistanceApi, Distances}

class DistanceCaching[F[_], P <: Path with CacheKey](cache: Cache[F, P, DistanceAndDuration], api: DistanceApi[F, P])(implicit F: MonadError[F, Throwable])
  extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    cache.caching(path, api.distance(path))

  override def distances(paths: List[P]): F[Map[P, Either[DistanceError, DistanceAndDuration]]] =
    paths
      .traverse(path => cache.get(path).map(path -> _))
      .flatMap(fallbackMissesFromApi(_, _.traverse { case (path, result) => cache.put(path, result)}))

  protected def fallbackMissesFromApi(results: List[(P, Option[DistanceAndDuration])], updateCache: List[(P, DistanceAndDuration)] => F[List[Any]]): F[Map[P, Either[DistanceError, DistanceAndDuration]]] = {
    val hits = results.collect { case (path, Some(result)) => path -> Right(result) }.toMap
    val misses = results.collect { case (path, None) => path }
    for {
      apiResults <- distances(misses)
      successes = apiResults.collect { case (path, Right(result)) => path -> result }.toList
      _ <- updateCache(successes)
    } yield hits ++ apiResults
  }
}

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
