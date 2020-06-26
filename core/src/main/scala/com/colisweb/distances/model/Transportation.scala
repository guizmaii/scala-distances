package com.colisweb.distances.model

import java.time.Instant

trait FixedSpeedTransportation {
  val speed: SpeedInKmH
}

trait TravelModeTransportation {
  val travelMode: TravelMode
}

trait DepartureTime {
  val departureTime: Option[Instant]
}
