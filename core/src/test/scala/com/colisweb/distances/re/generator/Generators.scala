package com.colisweb.distances.re.generator

import java.util.concurrent.TimeUnit

import com.colisweb.distances.re.generator.Boundaries._
import com.colisweb.distances.re.generator.PointBoundaries._
import com.colisweb.distances.re.model.Path.PathSimple
import com.colisweb.distances.re.model.{DistanceAndDuration, Path, Point}
import com.colisweb.distances.re.model.Point.{Latitude, Longitude}
import org.scalacheck.Gen
import squants.motion.{Distance, KilometersPerHour, Velocity}
import squants.space.Meters

import scala.concurrent.duration.FiniteDuration

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

  def genDistance: Gen[Distance] =
    Gen.chooseNum(0, maxDistanceMeters).map(Meters(_))

  def genDuration: Gen[FiniteDuration] =
    Gen.chooseNum(0, maxDurationSeconds).map(FiniteDuration(_, TimeUnit.SECONDS))

  def genDistanceAndDurationUnrelated: Gen[DistanceAndDuration] =
    for {
      distance <- genDistance
      duration <- genDuration
    } yield DistanceAndDuration(distance, duration)

  def genPathSimple: Gen[PathSimple] =
    for {
      origin      <- genPoint(World)
      destination <- genPoint(World)
    } yield Path(origin, destination)

  def genPathSimpleAndDistanceUnrelated: Gen[(PathSimple, DistanceAndDuration)] =
    for {
      path                <- genPathSimple
      distanceAndDuration <- genDistanceAndDurationUnrelated
    } yield (path, distanceAndDuration)

  def genPathSimpleAndDistanceUnrelatedSet: Gen[Map[PathSimple, DistanceAndDuration]] =
    Gen.listOf(genPathSimpleAndDistanceUnrelated).map(_.toMap)

  case class BatchSlice(index: Int, size: Int)
  def genBatchSlices: Gen[List[BatchSlice]] =
    for {
      indexes <- Gen.listOf(Gen.chooseNum[Int](0, 100, 1, 4, 10, 15)).map(_.distinct.sorted)
      sizes   <- Gen.listOfN(indexes.size, Gen.chooseNum(0, 100, 1, 2, 3, 5, 10, 20))
    } yield indexes.zip(sizes).map { case (index, size) => BatchSlice(index, size) }
}
