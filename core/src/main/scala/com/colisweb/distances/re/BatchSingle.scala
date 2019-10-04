package com.colisweb.distances.re

import cats.implicits._
import cats.{Monad, Parallel}
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class BatchSingle[F[_]: Monad: Parallel, E, O](private val singleApi: DistanceApi[F, E, O])
    extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    paths.parTraverse(path => singleApi.distance(path).map(path -> _)).map(_.toMap)
}
