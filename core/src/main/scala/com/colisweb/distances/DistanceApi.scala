package com.colisweb.distances

import com.colisweb.distances.model.{DistanceAndDuration, DistanceError, Path}

trait DistanceApi[F[_], P <: Path] {

  def distance(path: P): F[DistanceAndDuration]

  def distances(paths: List[P]): F[Map[P, Either[DistanceError, DistanceAndDuration]]]
}
