package com.colisweb.distances.re.util

import cats.Applicative
import cats.data.Kleisli
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import com.colisweb.distances.re.{DistanceApi, DistanceOptionApi, Distances}

class StaticDistanceApi[F[_]: Applicative, E](data: Map[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]])
    extends Distances.Function[F, E, TestTypes.IdParam] {

  override def distance(path: Path[TestTypes.IdParam]): F[Either[E, DistanceAndDuration]] =
    Applicative[F].pure(data(path))
}

object StaticDistanceApi {

  def apply[F[_]: Applicative, E](
      data: Map[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]]
  ): Distances.Builder[F, E, TestTypes.IdParam] = {
    val instance = new StaticDistanceApi(data)
    Kleisli(instance.distance)
  }

  def apply[F[_]: Applicative, E](
      entries: (Path[TestTypes.IdParam], Either[E, DistanceAndDuration])*
  ): Distances.Builder[F, E, TestTypes.IdParam] =
    apply(entries.toMap)

  def empty[F[_]: Applicative, E]: Distances.Builder[F, E, TestTypes.IdParam] =
    apply(Map.empty[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]])
}

class StaticDistanceOptionApi[F[_]: Applicative](data: Map[Path[TestTypes.IdParam], Option[DistanceAndDuration]])
    extends Distances.FunctionOption[F, TestTypes.IdParam] {

  override def distance(path: Path[TestTypes.IdParam]): F[Option[DistanceAndDuration]] =
    Applicative[F].pure(data(path))
}

object StaticDistanceOptionApi {

  def apply[F[_]: Applicative](
      data: Map[Path[TestTypes.IdParam], Option[DistanceAndDuration]]
  ): Distances.BuilderOption[F, TestTypes.IdParam] = {
    val instance = new StaticDistanceOptionApi(data)
    Kleisli(instance.distance)
  }

  def apply[F[_]: Applicative](
      entries: (Path[TestTypes.IdParam], Option[DistanceAndDuration])*
  ): Distances.BuilderOption[F, TestTypes.IdParam] =
    apply(entries.toMap)


  def empty[F[_]: Applicative]: Distances.BuilderOption[F, TestTypes.IdParam] =
    apply(Map.empty[Path[TestTypes.IdParam], Option[DistanceAndDuration]])
}
