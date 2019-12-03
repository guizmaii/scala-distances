package com.colisweb.distances.util

import cats.Monad
import cats.data.Kleisli
import com.colisweb.distances.Distances
import com.colisweb.distances.model.DistanceAndDuration
import com.colisweb.distances.model.Path.PathSimple
import com.colisweb.distances.builder

class FromMapDistances[F[_]](implicit F: Monad[F]) {
  import builder.sequential._

  def fromMap(data: Map[PathSimple, DistanceAndDuration]): Distances.BuilderOption[F, Unit] =
    Kleisli(path => F.pure(data.get(path)))

  def empty: Distances.BuilderOption[F, Unit] =
    fromMap(Map.empty[PathSimple, DistanceAndDuration])

  def fromMapOrError[E](error: => E, data: Map[PathSimple, DistanceAndDuration]): Distances.Builder[F, E, Unit] =
    fromMap(data).nonOptional(error)

  def emptyAndError[E](error: => E): Distances.Builder[F, E, Unit] =
    fromMapOrError(error, Map.empty[PathSimple, DistanceAndDuration])

  def batchFromMap(data: Map[PathSimple, DistanceAndDuration]): Distances.BuilderBatchOption[F, Unit] =
    fromMap(data).batched

  def batchEmpty: Distances.BuilderBatchOption[F, Unit] =
    empty.batched

  def batchFromMapOrError[E](
      error: => E,
      data: Map[PathSimple, DistanceAndDuration]
  ): Distances.BuilderBatch[F, E, Unit] =
    fromMapOrError(error, data).batched

  def batchEmptyAndError[E](error: => E): Distances.BuilderBatch[F, E, Unit] =
    emptyAndError(error).batched
}

object FromMapDistances {

  def apply[F[_]: Monad]: FromMapDistances[F] = new FromMapDistances[F]
}
