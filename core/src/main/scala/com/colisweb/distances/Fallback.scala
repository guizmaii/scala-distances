package com.colisweb.distances

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, DistanceError, Path}

case class Fallback[F[_], P <: Path](first: DistanceApi[F, P], second: DistanceApi[F, P])(implicit F: MonadError[F, Throwable])
  extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    first.distance(path).recoverWith {
      case _: DistanceError => second.distance(path)
    }

  override def distances(paths: List[P]): F[Map[P, Either[DistanceError, DistanceAndDuration]]] =
    first.distances(paths).flatMap { results =>
      val (successes, failures) = results.partition(_._2.isRight)
      distances(failures.keys.toList).map(successes ++ _)
    }
}
