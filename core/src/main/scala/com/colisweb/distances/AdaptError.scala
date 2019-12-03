package com.colisweb.distances

import cats.Functor
import cats.implicits._

object AdaptError {

  def apply[F[_]: Functor, E, E2, O](
      api: Distances.Builder[F, E, O]
  )(adaptation: E => E2): Distances.Builder[F, E2, O] =
    api.map(_.leftMap(adaptation))
}

object AdaptErrorBatch {

  def apply[F[_]: Functor, E, E2, O](
      api: Distances.BuilderBatch[F, E, O],
      adaptation: E => E2
  ): Distances.BuilderBatch[F, E2, O] =
    api.map(_.mapValues(_.leftMap(adaptation)))
}
