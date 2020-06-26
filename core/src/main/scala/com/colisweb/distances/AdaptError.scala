package com.colisweb.distances

import cats.Functor
import cats.implicits._
import com.colisweb.distances.model.Path

object AdaptError {

  def apply[F[_]: Functor, P <: Path, E, E2](
      api: Distances.Builder[F, P, E]
  )(adaptation: E => E2): Distances.Builder[F, P, E2] =
    api.map(_.leftMap(adaptation))
}

object AdaptErrorBatch {

  def apply[F[_]: Functor, P <: Path, E, E2](
      api: Distances.BuilderBatch[F, P, E],
      adaptation: E => E2
  ): Distances.BuilderBatch[F, P, E2] =
    api.map(_.mapValues(_.leftMap(adaptation)))
}
