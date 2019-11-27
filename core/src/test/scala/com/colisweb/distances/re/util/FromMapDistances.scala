package com.colisweb.distances.re.util

import cats.Monad
import cats.data.Kleisli
import com.colisweb.distances.re.Distances
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import com.colisweb.distances.re.util.TestTypes.IdParam

class FromMapDistances[F[_]](implicit F: Monad[F]) {

  def fromMap(data: Map[Path[TestTypes.IdParam], DistanceAndDuration]): Distances.BuilderOption[F, IdParam] =
    Kleisli(path => F.pure(data.get(path)))

  def fromMap(data: (Path[TestTypes.IdParam], DistanceAndDuration)*): Distances.BuilderOption[F, IdParam] =
    fromMap(data.toMap)

  def empty: Distances.BuilderOption[F, IdParam] =
    fromMap(Map.empty[Path[TestTypes.IdParam], DistanceAndDuration])

  def fromMapOrError[E](
      error: => E,
      data: Map[Path[TestTypes.IdParam], DistanceAndDuration]
  ): Distances.Builder[F, E, IdParam] = {
    import com.colisweb.distances.re.builder.base._
    fromMap(data).nonOptional(error)
  }

  def fromMapOrError[E](
      error: => E,
      data: (Path[TestTypes.IdParam], DistanceAndDuration)*
  ): Distances.Builder[F, E, IdParam] =
    fromMapOrError(error, data.toMap)

  def emptyAndError[E](error: => E): Distances.Builder[F, E, IdParam] =
    fromMapOrError(error, Map.empty[Path[TestTypes.IdParam], DistanceAndDuration])
}

object FromMapDistances {

  def apply[F[_]: Monad]: FromMapDistances[F] = new FromMapDistances[F]
}
