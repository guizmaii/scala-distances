package com.colisweb.distances.re.bird
import com.colisweb.distances.re.model.DistanceAndDuration.{DistanceInKilometers, DurationInSeconds}
import squants.Velocity
import squants.motion.Distance

import scala.concurrent.duration.{FiniteDuration, _}

object DurationFromSpeed {

  def durationForDistance(distance: DistanceInKilometers, velocity: Velocity): DurationInSeconds =
    ((3600.0 * distance) / velocity.toKilometersPerSecond).toLong

  def durationForDistance(distance: Distance, velocity: Velocity): FiniteDuration =
    durationForDistance(distance.toKilometers, velocity).seconds
}
