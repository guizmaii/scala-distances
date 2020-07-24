package com.colisweb.distances.bird

import cats.Applicative
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DistanceAndDuration, FixedSpeedTransportation, Path}

class HaversineDistanceProvider[F[_]: Applicative, P <: Path with FixedSpeedTransportation] extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] = {
    val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
    val distanceAndDuration  = DistanceAndDuration(distanceInKilometers, timeInSeconds)
    Applicative[F].pure(distanceAndDuration)
  }
}
