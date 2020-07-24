package com.colisweb.distances.util

import cats.MonadError
import com.colisweb.distances.model.{DistanceAndDuration, DistanceError, PathPlain}
import com.colisweb.distances.{DistanceApi, DistanceBuilder}

class FromMapDistances[F[_]](implicit F: MonadError[F, Throwable]) {

  def fromMapOrError(
      data: Map[PathPlain, DistanceAndDuration],
      error: => DistanceError
  ): DistanceBuilder[F, PathPlain] =
    DistanceBuilder(new FromMapDistanceApi(data, error))

  def fromMap(data: Map[PathPlain, DistanceAndDuration]): DistanceBuilder[F, PathPlain] =
    fromMapOrError(data, TestTypes.Error)

  def empty: DistanceBuilder[F, PathPlain] =
    fromMap(Map.empty[PathPlain, DistanceAndDuration])

  def emptyAndError(error: => DistanceError): DistanceBuilder[F, PathPlain] =
    fromMapOrError(Map.empty[PathPlain, DistanceAndDuration], error)

}

class FromMapDistanceApi[F[_]](data: Map[PathPlain, DistanceAndDuration], error: => DistanceError)(
    implicit F: MonadError[F, Throwable]
) extends DistanceApi[F, PathPlain] {

  override def distance(path: PathPlain): F[DistanceAndDuration] = data.get(path) match {
    case Some(distanceAndDuration) => F.pure(distanceAndDuration)
    case None                      => F.raiseError(error)
  }
}

object FromMapDistances {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): FromMapDistances[F] = new FromMapDistances[F]
}
