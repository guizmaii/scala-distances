package com.colisweb.distances

import cats.Monad
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object FallbackBatch {

  def apply[F[_]: Monad, E, R](
      first: Distances.BuilderBatch[F, _, R],
      second: Distances.BuilderBatch[F, E, R]
  ): Distances.BuilderBatch[F, E, R] =
    first.andThen { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[Path[R], Either[E, DistanceAndDuration]], List.empty[Path[R]])
      ) {
        case ((s, e), (path, Left(_)))      => (s, e :+ path)
        case ((s, e), (path, Right(value))) => (s.updated(path, Right(value)), e)
      }
      if (missing.nonEmpty) {
        second(missing).map(values ++ _)
      } else {
        Monad[F].pure(values)
      }
    }
}

object FallbackBatchOption {

  def apply[F[_]: Monad, E, R](
      first: Distances.BuilderBatchOption[F, R],
      second: Distances.BuilderBatch[F, E, R]
  ): Distances.BuilderBatch[F, E, R] =
    first.andThen { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[Path[R], Either[E, DistanceAndDuration]], List.empty[Path[R]])
      ) {
        case ((s, e), (path, None))        => (s, e :+ path)
        case ((s, e), (path, Some(value))) => (s.updated(path, Right(value)), e)
      }
      if (missing.nonEmpty) {
        second(missing).map(values ++ _)
      } else {
        Monad[F].pure(values)
      }
    }

}
