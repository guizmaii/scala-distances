package com.colisweb.distances.re
import cats.Monad
import cats.data.Kleisli

object Fallback {

  def apply[F[_]: Monad, E, R](
      first: Distances.Builder[F, _, R],
      second: Distances.Builder[F, E, R]
  ): Distances.Builder[F, E, R] =
    first.flatMap {
      case Right(value) => Kleisli.pure(Right(value))
      case Left(_)      => second
    }
}

object FallbackOption {

  def apply[F[_]: Monad, E, R](
      first: Distances.BuilderOption[F, R],
      second: Distances.Builder[F, E, R]
  ): Distances.Builder[F, E, R] =
    first.flatMap {
      case Some(value) => Kleisli.pure(Right(value))
      case None        => second
    }
}
