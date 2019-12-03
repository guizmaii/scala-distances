package com.colisweb.distances

import cats.data.Kleisli
import cats.implicits._
import cats.{Monad, Parallel}

object BatchSingle {

  def sequential[F[_]: Monad, E, O](singleDistance: Distances.Builder[F, E, O]): Distances.BuilderBatch[F, E, O] =
    Kleisli(paths => paths.traverse(path => singleDistance(path).map(path -> _)).map(_.toMap))

  def parallel[F[_]: Monad: Parallel, E, O](
      singleDistance: Distances.Builder[F, E, O]
  ): Distances.BuilderBatch[F, E, O] =
    Kleisli(paths => paths.parTraverse(path => singleDistance(path).map(path -> _)).map(_.toMap))
}

object BatchSingleOptional {

  def sequential[F[_]: Monad, R](singleDistance: Distances.BuilderOption[F, R]): Distances.BuilderBatchOption[F, R] =
    Kleisli(paths => paths.traverse(path => singleDistance(path).map(path -> _)).map(_.toMap))

  def parallel[F[_]: Monad: Parallel, R](
      singleDistance: Distances.BuilderOption[F, R]
  ): Distances.BuilderBatchOption[F, R] =
    Kleisli(paths => paths.parTraverse(path => singleDistance(path).map(path -> _)).map(_.toMap))
}
