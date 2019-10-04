package com.colisweb.distances.re.cache
import cats.Monad
import cats.implicits._
import com.colisweb.distances.re.DistanceApi
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class DistanceUpdateCache[F[_]: Monad, E, O](
    private val api: DistanceApi[F, E, O],
    private val cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
) extends DistanceApi[F, E, O] {

  override def distance(path: Path[O]): F[Either[E, DistanceAndDuration]] =
    api.distance(path).flatMap {
      case Right(value)   => cacheSet.set(path, value).map(_ => Right(value))
      case left @ Left(_) => Monad[F].pure(left)
    }
}

object DistanceUpdateCache {
  def apply[F[_]: Monad, E, O](
      api: DistanceApi[F, E, O],
      cacheSet: CacheSet[F, Path[O], DistanceAndDuration]
  ): DistanceUpdateCache[F, E, O] =
    new DistanceUpdateCache(api, cacheSet)
}
