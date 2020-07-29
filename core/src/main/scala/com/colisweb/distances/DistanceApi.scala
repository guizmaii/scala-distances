package com.colisweb.distances

import com.colisweb.distances.model.DistanceAndDuration

trait DistanceApi[F[_], P] {

  def distance(path: P): F[DistanceAndDuration]
}
