package com.colisweb.distances

import cats.Monad
import cats.data.Kleisli
import com.colisweb.distances.model.Path

object Fallback {

  def apply[F[_]: Monad, P <: Path, E](
      first: Distances.Builder[F, P, _],
      second: Distances.Builder[F, P, E]
  ): Distances.Builder[F, P, E] =
    first.flatMap {
      case Right(value) => Kleisli.pure(Right(value))
      case Left(_)      => second
    }
}

object FallbackOption {

  def apply[F[_]: Monad, P <: Path, E](
      first: Distances.BuilderOption[F, P],
      second: Distances.Builder[F, P, E]
  ): Distances.Builder[F, P, E] =
    first.flatMap {
      case Some(value) => Kleisli.pure(Right(value))
      case None        => second
    }
}
