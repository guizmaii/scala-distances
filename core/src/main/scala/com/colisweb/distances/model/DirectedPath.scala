package com.colisweb.distances.model

import java.time.Instant

import com.colisweb.distances.cache.CacheKey
import squants.motion.Velocity

final case class DirectedPath(origin: Point, destination: Point) extends OriginDestinationData

final case class DirectedPathWithSpeed(origin: Point, destination: Point, speed: SpeedInKmH)
    extends OriginDestinationData

final case class DirectedPathWithMode(origin: Point, destination: Point, travelMode: TravelMode)
    extends OriginDestinationData

final case class DirectedPathWithModeAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    departureTime: Option[Instant]
) extends OriginDestinationData

final case class DirectedPathWithModeAndSpeedAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    speed: SpeedInKmH,
    departureTime: Option[Instant]
) extends OriginDestinationData

object DirectedPath {

  def apply(origin: Point, destination: Point, speed: SpeedInKmH): DirectedPathWithSpeed =
    DirectedPathWithSpeed(origin, destination, speed)

  def apply(origin: Point, destination: Point, speed: Velocity): DirectedPathWithSpeed =
    DirectedPathWithSpeed(origin, destination, speed.toKilometersPerHour)

  def apply(origin: Point, destination: Point, travelMode: TravelMode): DirectedPathWithMode =
    DirectedPathWithMode(origin, destination, travelMode)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAt =
    DirectedPathWithModeAt(origin, destination, travelMode, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: SpeedInKmH,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAndSpeedAt =
    DirectedPathWithModeAndSpeedAt(origin, destination, travelMode, speed, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: Velocity,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAndSpeedAt =
    DirectedPathWithModeAndSpeedAt(origin, destination, travelMode, speed.toKilometersPerHour, departureTime)

  implicit val cacheKey: CacheKey[DirectedPath]                   = CacheKey.forProduct
  implicit val originDestination: OriginDestination[DirectedPath] = OriginDestination.forData
}

object DirectedPathWithSpeed {
  implicit val cacheKey: CacheKey[DirectedPathWithSpeed]                   = CacheKey.forProduct
  implicit val originDestination: OriginDestination[DirectedPathWithSpeed] = OriginDestination.forData
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithSpeed] =
    (path: DirectedPathWithSpeed) => path.speed
}

object DirectedPathWithMode {
  implicit val cacheKey: CacheKey[DirectedPathWithMode]                   = CacheKey.forProduct
  implicit val originDestination: OriginDestination[DirectedPathWithMode] = OriginDestination.forData
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithMode] =
    (path: DirectedPathWithMode) => path.travelMode
}

object DirectedPathWithModeAt {
  implicit val cacheKey: CacheKey[DirectedPathWithModeAt]                   = CacheKey.forProduct
  implicit val originDestination: OriginDestination[DirectedPathWithModeAt] = OriginDestination.forData
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAt] =
    (path: DirectedPathWithModeAt) => path.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAt] =
    (path: DirectedPathWithModeAt) => path.departureTime
}

object DirectedPathWithModeAndSpeedAt {
  implicit val cacheKey: CacheKey[DirectedPathWithModeAndSpeedAt]                   = CacheKey.forProduct
  implicit val originDestination: OriginDestination[DirectedPathWithModeAndSpeedAt] = OriginDestination.forData
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithModeAndSpeedAt] =
    (path: DirectedPathWithModeAndSpeedAt) => path.speed
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAndSpeedAt] =
    (path: DirectedPathWithModeAndSpeedAt) => path.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAndSpeedAt] =
    (path: DirectedPathWithModeAndSpeedAt) => path.departureTime
}
