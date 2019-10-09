package com.colisweb.distances.re

import cats.Functor
import cats.implicits._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class AdaptError[F[_]: Functor, E, E2, O](
    private val api: DistanceApi[F, E, O],
    private val adaptation: E => E2
) extends DistanceApi[F, E2, O] {

  override def distance(path: Path[O]): F[Either[E2, DistanceAndDuration]] =
    api.distance(path).map(_.leftMap(adaptation))
}

class AdaptErrorBatch[F[_]: Functor, E, E2, O](
    private val api: DistanceBatchApi[F, E, O],
    private val adaptation: E => E2
) extends DistanceBatchApi[F, E2, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E2, DistanceAndDuration]]] =
    api.distances(paths).map(_.mapValues(_.leftMap(adaptation)))
}

object AdaptError {

  def apply[F[_]: Functor, E, E2, O](api: DistanceApi[F, E, O], adaptation: E => E2): DistanceApi[F, E2, O] =
    new AdaptError(api, adaptation)

  def apply[F[_]: Functor, E, E2, O](api: DistanceBatchApi[F, E, O], adaptation: E => E2): DistanceBatchApi[F, E2, O] =
    new AdaptErrorBatch(api, adaptation)
}
