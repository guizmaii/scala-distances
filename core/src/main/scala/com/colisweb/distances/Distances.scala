package com.colisweb.distances

import cats.data.Kleisli
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object Distances {

  type Function[F[_], E, R] = Path[R] => F[Either[E, DistanceAndDuration]]
  type Builder[F[_], E, R]  = Kleisli[F, Path[R], Either[E, DistanceAndDuration]]

  type FunctionBatch[F[_], E, R] = List[Path[R]] => F[Map[Path[R], Either[E, DistanceAndDuration]]]
  type BuilderBatch[F[_], E, R]  = Kleisli[F, List[Path[R]], Map[Path[R], Either[E, DistanceAndDuration]]]

  type FunctionOption[F[_], R] = Path[R] => F[Option[DistanceAndDuration]]
  type BuilderOption[F[_], R]  = Kleisli[F, Path[R], Option[DistanceAndDuration]]

  type FunctionBatchOption[F[_], R] = List[Path[R]] => F[Map[Path[R], Option[DistanceAndDuration]]]
  type BuilderBatchOption[F[_], R]  = Kleisli[F, List[Path[R]], Map[Path[R], Option[DistanceAndDuration]]]
}
