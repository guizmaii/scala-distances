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

object AdaptError {
  def apply[F[_]: Functor, E, E2, O](api: DistanceApi[F, E, O], adaptation: E => E2): AdaptError[F, E, E2, O] =
    new AdaptError(api, adaptation)

}
