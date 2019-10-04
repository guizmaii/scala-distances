package com.colisweb.distances.re
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

trait DistanceApi[F[_], E, O] {

  def distance(path: Path[O]): F[Either[E, DistanceAndDuration]]
}

trait DistanceBatchApi[F[_], E, O] {

  // TODO: can be written using distances(List...)
  // def distances(path: DirectedPathMultipleModes): F[Map[TravelMode, Either[DistanceApiError, Distance]]]

  def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]]
}
