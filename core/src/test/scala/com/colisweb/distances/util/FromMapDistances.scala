package com.colisweb.distances.util

import cats.MonadError
import com.colisweb.distances.model.{DirectedPath, DistanceAndDuration, DistanceError}
import com.colisweb.distances.{DistanceApi, Distances}

class FromMapDistances[F[_]](implicit F: MonadError[F, Throwable]) {

  def fromMapOrError(
      data: Map[DirectedPath, DistanceAndDuration],
      error: => DistanceError
  ): Distances[F, DirectedPath] =
    Distances(new FromMapDistanceApi(data, error))

  def fromMap(data: Map[DirectedPath, DistanceAndDuration]): Distances[F, DirectedPath] =
    fromMapOrError(data, TestTypes.Error)

  def empty: Distances[F, DirectedPath] =
    fromMap(Map.empty[DirectedPath, DistanceAndDuration])

  def emptyAndError(error: => DistanceError): Distances[F, DirectedPath] =
    fromMapOrError(Map.empty[DirectedPath, DistanceAndDuration], error)

}

class FromMapDistanceApi[F[_]](data: Map[DirectedPath, DistanceAndDuration], error: => DistanceError)(
    implicit F: MonadError[F, Throwable]
) extends DistanceApi[F, DirectedPath] {

  override def distance(path: DirectedPath): F[DistanceAndDuration] = data.get(path) match {
    case Some(distanceAndDuration) => F.pure(distanceAndDuration)
    case None                      => F.raiseError(error)
  }
}

object FromMapDistances {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): FromMapDistances[F] = new FromMapDistances[F]
}
