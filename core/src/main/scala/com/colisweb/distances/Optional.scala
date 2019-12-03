package com.colisweb.distances

import cats.Functor

object Optional {

  def apply[F[_]: Functor, R](builder: Distances.Builder[F, _, R]): Distances.BuilderOption[F, R] =
    builder.map(_.toOption)
}

object OptionalBatch {

  def apply[F[_]: Functor, R](builder: Distances.BuilderBatch[F, _, R]): Distances.BuilderBatchOption[F, R] =
    builder.map(_.mapValues(_.toOption))
}

object NonOptional {

  def apply[F[_]: Functor, E, R](builder: Distances.BuilderOption[F, R])(error: => E): Distances.Builder[F, E, R] =
    builder.map(_.toRight(error))
}

object NonOptionalBatch {

  def apply[F[_]: Functor, E, R](
      builder: Distances.BuilderBatchOption[F, R]
  )(error: => E): Distances.BuilderBatch[F, E, R] =
    builder.map(_.mapValues(_.toRight(error)))
}
