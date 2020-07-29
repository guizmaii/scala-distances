package com.colisweb.distances.model

import java.time.Instant

trait FixedSpeedTransportation[P] {
  def speed(path: P): SpeedInKmH
}

trait TravelModeTransportation[P] {
  def travelMode(path: P): TravelMode
}

trait DepartureTime[P] {
  def departureTime(path: P): Option[Instant]
}
