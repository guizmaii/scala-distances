package com.colisweb.distances

import com.colisweb.distances.model.{DistanceAndDuration, Path}

trait DistanceApi[F[_], P <: Path] {

  def distance(path: P): F[DistanceAndDuration]
}
