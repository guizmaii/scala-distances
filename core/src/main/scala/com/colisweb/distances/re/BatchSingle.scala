package com.colisweb.distances.re

import cats.implicits._
import cats.{Monad, Parallel}
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class BatchSingleSequential[F[_]: Monad, E, O](private val singleApi: DistanceApi[F, E, O])
    extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    paths.traverse(path => singleApi.distance(path).map(path -> _)).map(_.toMap)
}

class BatchSingleParallel[F[_]: Monad: Parallel, E, O](private val singleApi: DistanceApi[F, E, O])
    extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    paths.parTraverse(path => singleApi.distance(path).map(path -> _)).map(_.toMap)
}

object BatchSingle {

  def sequential[F[_]: Monad, E, O](singleApi: DistanceApi[F, E, O]): DistanceBatchApi[F, E, O] =
    new BatchSingleSequential[F, E, O](singleApi)

  def parallel[F[_]: Monad: Parallel, E, O](singleApi: DistanceApi[F, E, O]): DistanceBatchApi[F, E, O] =
    new BatchSingleParallel[F, E, O](singleApi)
}

class BatchSingleOptionalSequential[F[_]: Monad, O](private val singleApi: DistanceOptionApi[F, O])
    extends DistanceBatchOptionApi[F, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Option[DistanceAndDuration]]] =
    paths.traverse(path => singleApi.distance(path).map(path -> _)).map(_.toMap)
}

class BatchSingleOptionalParallel[F[_]: Monad: Parallel, O](private val singleApi: DistanceOptionApi[F, O])
    extends DistanceBatchOptionApi[F, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Option[DistanceAndDuration]]] =
    paths.parTraverse(path => singleApi.distance(path).map(path -> _)).map(_.toMap)
}

object BatchSingleOptional {

  def sequential[F[_]: Monad, O](singleApi: DistanceOptionApi[F, O]): DistanceBatchOptionApi[F, O] =
    new BatchSingleOptionalSequential[F, O](singleApi)

  def parallel[F[_]: Monad: Parallel, O](singleApi: DistanceOptionApi[F, O]): DistanceBatchOptionApi[F, O] =
    new BatchSingleOptionalParallel[F, O](singleApi)
}
