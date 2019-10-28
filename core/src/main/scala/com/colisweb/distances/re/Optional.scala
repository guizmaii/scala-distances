package com.colisweb.distances.re

import cats.Functor

object Optional {

  def apply[F[_]: Functor, R](api: DistanceApi.Builder[F, _, R]): Distances.BuilderOption[F, R] =
    api.map(_.toOption)

  def apply[F[_]: Functor, R](api: DistanceBatchApi.Builder[F, _, R]): Distances.BuilderBatchOption[F, R] =
    api.map(_.mapValues(_.toOption))
}
