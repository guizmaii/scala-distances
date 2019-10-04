package com.colisweb.distances.re

import cats.Functor
import cats.implicits._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

trait DistanceOptionApi[F[_], O] {

  def distance(path: Path[O]): F[Option[DistanceAndDuration]]
}

class Optional[F[_]: Functor, O](private val api: DistanceApi[F, _, O]) extends DistanceOptionApi[F, O] {

  override def distance(path: Path[O]): F[Option[DistanceAndDuration]] =
    api.distance(path).map(_.toOption)
}
