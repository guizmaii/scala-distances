package com.colisweb.distances.re

import cats.Functor
import cats.implicits._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

trait DistanceOptionApi[F[_], O] {

  def distance(path: Path[O]): F[Option[DistanceAndDuration]]
}

trait DistanceBatchOptionApi[F[_], O] {

  def distances(paths: List[Path[O]]): F[Map[Path[O], Option[DistanceAndDuration]]]
}

class Optional[F[_]: Functor, O](private val api: DistanceApi[F, _, O]) extends DistanceOptionApi[F, O] {

  override def distance(path: Path[O]): F[Option[DistanceAndDuration]] =
    api.distance(path).map(_.toOption)
}

class OptionalBatch[F[_]: Functor, O](private val api: DistanceBatchApi[F, _, O]) extends DistanceBatchOptionApi[F, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Option[DistanceAndDuration]]] =
    api.distances(paths).map(_.mapValues(_.toOption))
}
