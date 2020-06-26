package com.colisweb.distances

import cats.data.Kleisli
import cats.implicits._
import cats.{Monad, Parallel}
import com.colisweb.distances.model.Path

object BatchSingle {

  def sequential[F[_]: Monad, P <: Path, E](
      singleDistance: Distances.Builder[F, P, E]
  ): Distances.BuilderBatch[F, P, E] =
    Kleisli(paths => paths.traverse(path => singleDistance(path).map(path -> _)).map(_.toMap))

  def parallel[F[_]: Monad: Parallel, P <: Path, E](
      singleDistance: Distances.Builder[F, P, E]
  ): Distances.BuilderBatch[F, P, E] =
    Kleisli(paths => paths.parTraverse(path => singleDistance(path).map(path -> _)).map(_.toMap))
}

object BatchSingleOptional {

  def sequential[F[_]: Monad, P <: Path](
      singleDistance: Distances.BuilderOption[F, P]
  ): Distances.BuilderBatchOption[F, P] =
    Kleisli(paths => paths.traverse(path => singleDistance(path).map(path -> _)).map(_.toMap))

  def parallel[F[_]: Monad: Parallel, P <: Path](
      singleDistance: Distances.BuilderOption[F, P]
  ): Distances.BuilderBatchOption[F, P] =
    Kleisli(paths => paths.parTraverse(path => singleDistance(path).map(path -> _)).map(_.toMap))
}
