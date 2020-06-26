package com.colisweb.distances

import cats.Monad
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object FallbackBatch {

  def apply[F[_]: Monad, P <: Path, E](
      first: Distances.BuilderBatch[F, P, _],
      second: Distances.BuilderBatch[F, P, E]
  ): Distances.BuilderBatch[F, P, E] =
    first.andThen { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[P, Either[E, DistanceAndDuration]], List.empty[P])
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

  def apply[F[_]: Monad, P <: Path, E](
      first: Distances.BuilderBatchOption[F, P],
      second: Distances.BuilderBatch[F, P, E]
  ): Distances.BuilderBatch[F, P, E] =
    first.andThen { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[P, Either[E, DistanceAndDuration]], List.empty[P])
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
