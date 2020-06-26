package com.colisweb.distances.util

import cats.Monad
import cats.data.Kleisli
import com.colisweb.distances.{Distances, builder}
import com.colisweb.distances.model.{DistanceAndDuration, PathPlain}

class FromMapDistances[F[_]](implicit F: Monad[F]) {
  private val builders = builder.builders[F]
  import builders._

  def fromMap(data: Map[PathPlain, DistanceAndDuration]): Distances.BuilderOption[F, PathPlain] =
    Kleisli(path => F.pure(data.get(path)))

  def empty: Distances.BuilderOption[F, PathPlain] =
    fromMap(Map.empty[PathPlain, DistanceAndDuration])

  def fromMapOrError[E](error: => E, data: Map[PathPlain, DistanceAndDuration]): Distances.Builder[F, PathPlain, E] =
    fromMap(data).nonOptional(error)

  def emptyAndError[E](error: => E): Distances.Builder[F, PathPlain, E] =
    fromMapOrError(error, Map.empty[PathPlain, DistanceAndDuration])

  def batchFromMap(data: Map[PathPlain, DistanceAndDuration]): Distances.BuilderBatchOption[F, PathPlain] =
    fromMap(data).batched

  def batchEmpty: Distances.BuilderBatchOption[F, PathPlain] =
    empty.batched

  def batchFromMapOrError[E](
      error: => E,
      data: Map[PathPlain, DistanceAndDuration]
  ): Distances.BuilderBatch[F, PathPlain, E] =
    fromMapOrError(error, data).batched

  def batchEmptyAndError[E](error: => E): Distances.BuilderBatch[F, PathPlain, E] =
    emptyAndError(error).batched
}

object FromMapDistances {

  def apply[F[_]: Monad]: FromMapDistances[F] = new FromMapDistances[F]
}
