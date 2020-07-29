package com.colisweb.distances.generator

import com.colisweb.distances.generator.Boundaries._
import com.colisweb.distances.generator.PointBoundaries._
import com.colisweb.distances.model._
import org.scalacheck.Gen

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

  def genSpeed: Gen[SpeedInKmH] =
    Gen.chooseNum(minSpeedKmh, lightSpeedKmh)

  def genDistance: Gen[DistanceInKm] =
    Gen.chooseNum(0d, maxDistanceKilometers)

  def genDuration: Gen[DurationInSeconds] =
    Gen.chooseNum(0, maxDurationSeconds)

  def genDistanceAndDurationUnrelated: Gen[DistanceAndDuration] =
    for {
      distance <- genDistance
      duration <- genDuration
    } yield DistanceAndDuration(distance, duration)

  def genPathBetween: Gen[DirectedPath] =
    for {
      origin      <- genPoint(World)
      destination <- genPoint(World)
    } yield DirectedPath(origin, destination)

  def genPathSimpleAndDistanceUnrelated: Gen[(DirectedPath, DistanceAndDuration)] =
    for {
      path                <- genPathBetween
      distanceAndDuration <- genDistanceAndDurationUnrelated
    } yield (path, distanceAndDuration)

  def genPathSimpleAndDistanceUnrelatedSet: Gen[Map[DirectedPath, DistanceAndDuration]] =
    Gen.listOf(genPathSimpleAndDistanceUnrelated).map(_.toMap)

  case class BatchSlice(index: Int, size: Int)
  def genBatchSlices: Gen[List[BatchSlice]] =
    for {
      indexes <- Gen.listOf(Gen.chooseNum[Int](0, 100, 1, 4, 10, 15)).map(_.distinct.sorted)
      sizes   <- Gen.listOfN(indexes.size, Gen.chooseNum(0, 100, 1, 2, 3, 5, 10, 20))
    } yield indexes.zip(sizes).map { case (index, size) => BatchSlice(index, size) }
}
