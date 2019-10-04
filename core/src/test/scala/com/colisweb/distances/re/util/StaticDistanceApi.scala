package com.colisweb.distances.re.util

import cats.Applicative
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import com.colisweb.distances.re.{DistanceApi, DistanceOptionApi}

class StaticDistanceApi[F[_]: Applicative, E](data: Map[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]])
    extends DistanceApi[F, E, TestTypes.IdParam] {

  override def distance(path: Path[TestTypes.IdParam]): F[Either[E, DistanceAndDuration]] =
    Applicative[F].pure(data(path))
}

object StaticDistanceApi {

  def apply[F[_]: Applicative, E](
      data: Map[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]]
  ): StaticDistanceApi[F, E] =
    new StaticDistanceApi(data)

  def apply[F[_]: Applicative, E](
      entries: (Path[TestTypes.IdParam], Either[E, DistanceAndDuration])*
  ): StaticDistanceApi[F, E] =
    apply(entries.toMap)

  def empty[F[_]: Applicative, E]: StaticDistanceApi[F, E] =
    apply(Map.empty[Path[TestTypes.IdParam], Either[E, DistanceAndDuration]])
}

class StaticDistanceOptionApi[F[_]: Applicative](data: Map[Path[TestTypes.IdParam], Option[DistanceAndDuration]])
    extends DistanceOptionApi[F, TestTypes.IdParam] {

  override def distance(path: Path[TestTypes.IdParam]): F[Option[DistanceAndDuration]] =
    Applicative[F].pure(data(path))
}

object StaticDistanceOptionApi {

  def apply[F[_]: Applicative](
      data: Map[Path[TestTypes.IdParam], Option[DistanceAndDuration]]
  ): StaticDistanceOptionApi[F] =
    new StaticDistanceOptionApi(data)

  def apply[F[_]: Applicative](
      entries: (Path[TestTypes.IdParam], Option[DistanceAndDuration])*
  ): StaticDistanceOptionApi[F] =
    apply(entries.toMap)


  def empty[F[_]: Applicative]: StaticDistanceOptionApi[F] =
    apply(Map.empty[Path[TestTypes.IdParam], Option[DistanceAndDuration]])
}
