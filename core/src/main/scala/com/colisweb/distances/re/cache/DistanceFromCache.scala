package com.colisweb.distances.re.cache

import com.colisweb.distances.re.DistanceOptionApi
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class DistanceFromCache[F[_], O](private val cache: CacheGet[F, Path[O], DistanceAndDuration])
    extends DistanceOptionApi[F, O] {

  override def distance(path: Path[O]): F[Option[DistanceAndDuration]] =
    cache.get(path)
}

object DistanceFromCache {
  def apply[F[_], O](cache: CacheGet[F, Path[O], DistanceAndDuration]): DistanceOptionApi[F, O] =
    new DistanceFromCache(cache)
}
