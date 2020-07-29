package com.colisweb.distances.model

import java.time.Instant

object syntax {

  implicit class OriginDestinationSyntax[P](path: P)(implicit OD: OriginDestination[P]) {
    def origin: Point      = OD.origin(path)
    def destination: Point = OD.destination(path)
  }

  implicit class FixedSpeedTransportationSyntax[P](path: P)(implicit FS: FixedSpeedTransportation[P]) {
    def speed: SpeedInKmH = FS.speed(path)
  }

  implicit class TravelModeTransportationSyntax[P](path: P)(implicit TM: TravelModeTransportation[P]) {
    def travelMode: TravelMode = TM.travelMode(path)
  }

  implicit class DepartureTimeSyntax[P](path: P)(implicit DT: DepartureTime[P]) {
    def departureTime: Option[Instant] = DT.departureTime(path)
  }
}
