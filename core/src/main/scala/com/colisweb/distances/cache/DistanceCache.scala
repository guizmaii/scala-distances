package com.colisweb.distances.cache

import cats.implicits._
import cats.MonadError
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.DistanceAndDuration

import scala.util.control.NoStackTrace

case class DistanceFromCache[F[_], P](
    cache: Cache[F, P, DistanceAndDuration]
)(implicit F: MonadError[F, Throwable])
    extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    cache.get(path).flatMap {
      case Some(value) => F.pure(value)
      case None        => F.raiseError(CacheMissError(path))
    }
}

final case class CacheMissError(key: Any) extends RuntimeException(s"No entry in cache for $key") with NoStackTrace

case class DistanceWithCache[F[_], P](
    cache: Cache[F, P, DistanceAndDuration],
    api: DistanceApi[F, P]
) extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    cache.caching(path, api.distance(path))
}
