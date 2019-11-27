package com.colisweb.distances.re.generator

import com.colisweb.distances.re.generator.Boundaries._
import com.colisweb.distances.re.model.Point
import com.colisweb.distances.re.model.Point.{Latitude, Longitude}
import org.scalacheck.Gen
import squants.motion.{KilometersPerHour, Velocity}

object Generators {

  def genLongitude(boundaries: PointBoundaries): Gen[Longitude] =
    Gen.chooseNum(boundaries.bottomLeft.longitude, boundaries.topRight.longitude)

  def genLatitude(boundaries: PointBoundaries): Gen[Latitude] =
    Gen.chooseNum(boundaries.bottomLeft.latitude, boundaries.topRight.latitude)

  def genPoint(boundaries: PointBoundaries): Gen[Point] =
    for {
      latitude  <- genLatitude(boundaries)
      longitude <- genLongitude(boundaries)
    } yield Point(latitude, longitude)

  def genThreeSuccessiveValuesInDirection(min: Double, max: Double): Gen[(Double, Double, Double)] =
    Gen
      .chooseNum(min, max)
      .flatMap(
        pointValue =>
          Gen.oneOf(min, max).flatMap {
            case `max` =>
              for {
                closerShift  <- Gen.chooseNum(0, max / 2)
                fartherShift <- Gen.chooseNum(closerShift, max / 2)
              } yield (pointValue, pointValue + closerShift, pointValue + fartherShift)
            case `min` =>
              for {
                closerShift  <- Gen.chooseNum(min / 2, 0)
                fartherShift <- Gen.chooseNum(min / 2, closerShift)
              } yield (pointValue, pointValue + closerShift, pointValue + fartherShift)
          }
      )

  def genSpeed: Gen[Velocity] =
    Gen.chooseNum(minSpeedKmh, lightSpeedKmh).map(d => KilometersPerHour(d))

}
