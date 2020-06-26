package com.colisweb.distances.model

import squants.motion.Distance
import squants.space.Kilometers

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

final case class DistanceAndDuration(distance: DistanceInKm, duration: DurationInSeconds) {

  def distanceWithUnit: Distance       = Kilometers(distance)
  def durationWithUnit: FiniteDuration = duration.seconds
}
