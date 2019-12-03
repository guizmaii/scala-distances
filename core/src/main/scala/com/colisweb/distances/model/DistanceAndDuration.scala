package com.colisweb.distances.model

import squants.motion.Distance

import scala.concurrent.duration.FiniteDuration

case class DistanceAndDuration(distance: Distance, duration: FiniteDuration)

object DistanceAndDuration {

  type DistanceInKilometers = Double
  type DurationInSeconds    = Long
}
