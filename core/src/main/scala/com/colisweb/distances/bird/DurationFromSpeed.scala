package com.colisweb.distances.bird

import com.colisweb.distances.model.{DistanceInKm, DurationInSeconds, SpeedInKmH}
import squants.Velocity
import squants.motion.Distance

import scala.concurrent.duration.{FiniteDuration, _}

object DurationFromSpeed {

  def durationForDistance(distance: DistanceInKm, speed: SpeedInKmH): DurationInSeconds =
    ((3600.0 * distance) / speed).toLong

  def durationForDistance(distance: Distance, speed: Velocity): FiniteDuration =
    durationForDistance(distance.toKilometers, speed.toKilometersPerHour).seconds
}
