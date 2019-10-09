package com.colisweb.distances.re.cache
import cats.{Monad, Parallel}
import cats.implicits._
import com.colisweb.distances.re.{DistanceApi, DistanceBatchApi}
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class DistanceUpdateCache[F[_]: Monad, E, O](
    private val api: DistanceApi[F, E, O],
    private val cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
) extends DistanceApi[F, E, O] {

  override def distance(path: Path[O]): F[Either[E, DistanceAndDuration]] =
    api.distance(path).flatMap {
      case Right(value)   => cacheSet.set(path, value).map(_ => Right(value))
      case left @ Left(_) => Monad[F].pure(left)
    }
}

object DistanceUpdateCache {

  def apply[F[_]: Monad, E, O](
      api: DistanceApi[F, E, O],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
  ): DistanceUpdateCache[F, E, O] =
    new DistanceUpdateCache(api, cacheSet)
}

class DistanceUpdateCacheBatchSequential[F[_]: Monad, E, O](
    private val api: DistanceBatchApi[F, E, O],
    private val cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
) extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    api.distances(paths).flatMap { results =>
      results
        .collect { case (path, Right(value)) => path -> value }
        .toList
        .traverse { case (path, value) => cacheSet.set(path, value) }
        .map(_ => results)
    }
}

class DistanceUpdateCacheBatchParallel[F[_]: Monad: Parallel, E, O](
    private val api: DistanceBatchApi[F, E, O],
    private val cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
) extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    api.distances(paths).flatMap { results =>
      results
        .collect { case (path, Right(value)) => path -> value }
        .toList
        .parTraverse { case (path, value) => cacheSet.set(path, value) }
        .map(_ => results)
    }
}

object DistanceUpdateCacheBatch {

  def sequential[F[_]: Monad, E, O](
      api: DistanceBatchApi[F, E, O],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
  ): DistanceBatchApi[F, E, O] =
    new DistanceUpdateCacheBatchSequential(api, cacheSet)

  def parallel[F[_]: Monad: Parallel, E, O](
      api: DistanceBatchApi[F, E, O],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
  ): DistanceBatchApi[F, E, O] =
    new DistanceUpdateCacheBatchParallel(api, cacheSet)
}
