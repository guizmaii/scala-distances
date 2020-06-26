package com.colisweb.distances

import cats.Functor
import com.colisweb.distances.model.Path

object Optional {

  def apply[F[_]: Functor, P <: Path](builder: Distances.Builder[F, P, _]): Distances.BuilderOption[F, P] =
    builder.map(_.toOption)
}

object OptionalBatch {

  def apply[F[_]: Functor, P <: Path](builder: Distances.BuilderBatch[F, P, _]): Distances.BuilderBatchOption[F, P] =
    builder.map(_.mapValues(_.toOption))
}

object NonOptional {

  def apply[F[_]: Functor, P <: Path, E](
      builder: Distances.BuilderOption[F, P]
  )(error: => E): Distances.Builder[F, P, E] =
    builder.map(_.toRight(error))
}

object NonOptionalBatch {

  def apply[F[_]: Functor, P <: Path, E](
      builder: Distances.BuilderBatchOption[F, P]
  )(error: => E): Distances.BuilderBatch[F, P, E] =
    builder.map(_.mapValues(_.toRight(error)))
}
