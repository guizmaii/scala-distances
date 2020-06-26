package com.colisweb.distances.generator

import com.colisweb.distances.model._

import scala.concurrent.duration._

object Boundaries {

  val minSpeedKmh: SpeedInKmH               = 0.0d
  val lightSpeedKmh: SpeedInKmH             = 300000.0d * 3600
  val maxDistanceKilometers: DistanceInKm   = 12000d
  val maxDurationSeconds: DurationInSeconds = 72.hours.toSeconds

  val latitudeMin: Latitude   = -90.0d
  val latitudeMax: Latitude   = 90.0d
  val longitudeMin: Longitude = -180.0d
  val longitudeMax: Longitude = 180.0d
}

case class PointBoundaries(bottomLeft: Point, topRight: Point)
object PointBoundaries {
  import Boundaries._
  val IleDeFrance = PointBoundaries(bottomLeft = Point(48.487879, 1.872937), topRight = Point(49.074188, 3.130522))
  val France      = PointBoundaries(bottomLeft = Point(43.750857, -0.900897), topRight = Point(49.450930, 5.699994))
  val World =
    PointBoundaries(bottomLeft = Point(latitudeMin, longitudeMin), topRight = Point(latitudeMax, longitudeMax))
}
