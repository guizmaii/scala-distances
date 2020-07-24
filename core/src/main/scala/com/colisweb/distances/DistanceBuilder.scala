package com.colisweb.distances

import cats.MonadError
import com.colisweb.distances.model.Path

case class DistanceBuilder[F[_], P <: Path](api: DistanceApi[F, P]) {

  def build: DistanceApi[F, P] = api

  def fallback(other: DistanceBuilder[F, P])(implicit F: MonadError[F, Throwable]): DistanceBuilder[F, P] =
    copy(api = Fallback(api, other.api))
}
