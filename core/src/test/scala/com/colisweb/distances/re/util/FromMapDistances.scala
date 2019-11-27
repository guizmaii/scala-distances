package com.colisweb.distances.re.util

import cats.Monad
import cats.data.Kleisli
import com.colisweb.distances.re.Distances
import com.colisweb.distances.re.model.DistanceAndDuration
import com.colisweb.distances.re.model.Path.PathSimple

class FromMapDistances[F[_]](implicit F: Monad[F]) {

  def fromMap(data: Map[PathSimple, DistanceAndDuration]): Distances.BuilderOption[F, Unit] =
    Kleisli(path => F.pure(data.get(path)))

  def fromMap(data: (PathSimple, DistanceAndDuration)*): Distances.BuilderOption[F, Unit] =
    fromMap(data.toMap)

  def empty: Distances.BuilderOption[F, Unit] =
    fromMap(Map.empty[PathSimple, DistanceAndDuration])

  def fromMapOrError[E](
      error: => E,
      data: Map[PathSimple, DistanceAndDuration]
  ): Distances.Builder[F, E, Unit] = {
    import com.colisweb.distances.re.builder.base._
    fromMap(data).nonOptional(error)
  }

  def fromMapOrError[E](
      error: => E,
      data: (PathSimple, DistanceAndDuration)*
  ): Distances.Builder[F, E, Unit] =
    fromMapOrError(error, data.toMap)

  def emptyAndError[E](error: => E): Distances.Builder[F, E, Unit] =
    fromMapOrError(error, Map.empty[PathSimple, DistanceAndDuration])
}

object FromMapDistances {

  def apply[F[_]: Monad]: FromMapDistances[F] = new FromMapDistances[F]
}
