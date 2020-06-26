package com.colisweb.distances.bird

import cats.Applicative
import cats.data.Kleisli
import com.colisweb.distances.Distances
import com.colisweb.distances.model.{DistanceAndDuration, FixedSpeedTransportation, Path}

object HaversineDistanceProvider {

  def builder[F[_]: Applicative, P <: Path with FixedSpeedTransportation]: Distances.Builder[F, P, Nothing] = {
    Kleisli { path =>
      val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
      val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
      val distanceAndDuration  = DistanceAndDuration(distanceInKilometers, timeInSeconds)
      Applicative[F].pure(Right(distanceAndDuration))
    }
  }
}
