package com.colisweb.distances.generators

import com.colisweb.distances.model.DistanceAndDuration
import org.scalacheck.Gen
import squants.Length
import squants.space.LengthConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

object Gens {

  final val nameGen: Gen[String] = Gen.oneOf("toto", "tata", "titi", "tutu")
  final val ageGen: Gen[Int]     = Gen.choose(0, 150)

  final val lengthGen: Gen[Length]     = Gen.posNum[Double].map(_ meters)
  final val durationGen: Gen[Duration] = Gen.posNum[Int].map(_ seconds)

  final val distanceGen: Gen[DistanceAndDuration] = for {
    length   <- lengthGen
    duration <- durationGen
  } yield DistanceAndDuration(length.toKilometers, duration.toSeconds)
  /*
  final val PointGen: Gen[Point] = for {
    lat: Double  <- Gen.posNum[Double]
    long: Double <- Gen.posNum[Double]
  } yield Point(latitude = lat, longitude = long)

  import enumeratum.scalacheck._

  final val travelModeGen: Gen[TravelMode] = implicitly[Arbitrary[TravelMode]].arbitrary


   */
  /* final val directedPathGen: Gen[DirectedPathMultipleModes] = for {
    origin      <- PointGen
    destination <- PointGen
    travelModes <- Gen.nonEmptyListOf(travelModeGen)
  } yield
    DirectedPathMultipleModes(
      origin = origin,
      destination = destination,
      travelModes = travelModes
    )

   */
  /*
  final val totoGen: Gen[Toto] = for {
    name     <- nameGen
    age      <- ageGen
    Point  <- PointGen
    distance <- distanceGen
  } yield
    Toto(
      name = name,
      age = age,
      Point = Point,
      distance = distance
    )

 */
}
