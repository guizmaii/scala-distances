package com.colisweb.distances.model

import java.time.Instant

import com.colisweb.distances.cache.ProductCacheKey
import squants.motion.Velocity

trait Path {
  val origin: Point
  val destination: Point
}

final case class PathPlain(origin: Point, destination: Point) extends Path with ProductCacheKey

final case class PathWithSpeed(origin: Point, destination: Point, speed: SpeedInKmH)
    extends Path
    with FixedSpeedTransportation
    with ProductCacheKey

final case class PathWithMode(origin: Point, destination: Point, travelMode: TravelMode)
    extends Path
    with TravelModeTransportation
    with ProductCacheKey

final case class PathWithModeAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    departureTime: Option[Instant]
) extends Path
    with TravelModeTransportation
    with DepartureTime
    with ProductCacheKey

final case class PathWithModeAndSpeedAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    speed: SpeedInKmH,
    departureTime: Option[Instant]
) extends Path
    with TravelModeTransportation
    with FixedSpeedTransportation
    with DepartureTime
    with ProductCacheKey

object Path {

  def apply(origin: Point, destination: Point): PathPlain = PathPlain(origin, destination)

  def apply(origin: Point, destination: Point, speed: SpeedInKmH): PathWithSpeed =
    PathWithSpeed(origin, destination, speed)

  def apply(origin: Point, destination: Point, speed: Velocity): PathWithSpeed =
    PathWithSpeed(origin, destination, speed.toKilometersPerHour)

  def apply(origin: Point, destination: Point, travelMode: TravelMode): PathWithMode =
    PathWithMode(origin, destination, travelMode)

  def apply(origin: Point, destination: Point, travelMode: TravelMode, departureTime: Option[Instant]): PathWithModeAt =
    PathWithModeAt(origin, destination, travelMode, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: SpeedInKmH,
      departureTime: Option[Instant]
  ): PathWithModeAndSpeedAt =
    PathWithModeAndSpeedAt(origin, destination, travelMode, speed, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: Velocity,
      departureTime: Option[Instant]
  ): PathWithModeAndSpeedAt =
    PathWithModeAndSpeedAt(origin, destination, travelMode, speed.toKilometersPerHour, departureTime)
}
