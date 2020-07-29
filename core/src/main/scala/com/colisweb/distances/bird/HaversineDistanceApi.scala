package com.colisweb.distances.bird

import cats.Applicative
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DistanceAndDuration, FixedSpeedTransportation, OriginDestination}

class HaversineDistanceApi[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]
    extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[DistanceAndDuration] = {
    val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
    val distanceAndDuration  = DistanceAndDuration(distanceInKilometers, timeInSeconds)
    Applicative[F].pure(distanceAndDuration)
  }
}
