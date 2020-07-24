package com.colisweb.distances

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, DistanceError, Path}

case class Fallback[F[_], P <: Path](first: DistanceApi[F, P], second: DistanceApi[F, P])(
    implicit F: MonadError[F, Throwable]
) extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    first.distance(path).recoverWith {
      case _: DistanceError => second.distance(path)
    }
}
