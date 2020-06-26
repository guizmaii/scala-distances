package com.colisweb.distances

import cats.data.Kleisli
import com.colisweb.distances.model.{DistanceAndDuration, Path}

object Distances {

  type Function[F[_], P <: Path, E] = P => F[Either[E, DistanceAndDuration]]
  type Builder[F[_], P <: Path, E]  = Kleisli[F, P, Either[E, DistanceAndDuration]]

  type FunctionBatch[F[_], P <: Path, E] = List[P] => F[Map[P, Either[E, DistanceAndDuration]]]
  type BuilderBatch[F[_], P <: Path, E]  = Kleisli[F, List[P], Map[P, Either[E, DistanceAndDuration]]]

  type FunctionOption[F[_], P <: Path] = P => F[Option[DistanceAndDuration]]
  type BuilderOption[F[_], P <: Path]  = Kleisli[F, P, Option[DistanceAndDuration]]

  type FunctionBatchOption[F[_], P <: Path] = List[P] => F[Map[P, Option[DistanceAndDuration]]]
  type BuilderBatchOption[F[_], P <: Path]  = Kleisli[F, List[P], Map[P, Option[DistanceAndDuration]]]
}
