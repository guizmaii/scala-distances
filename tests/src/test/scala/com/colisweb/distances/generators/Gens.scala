package com.colisweb.distances.generators

import com.colisweb.distances.Types._
import com.colisweb.distances.caches.Toto
import com.colisweb.distances.re.model.TravelMode
import org.scalacheck.{Arbitrary, Gen}
import squants.Length
import squants.space.LengthConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

object Gens {

  final val nameGen: Gen[String] = Gen.oneOf("toto", "tata", "titi", "tutu")
  final val ageGen: Gen[Int]     = Gen.choose(0, 150)

  final val lengthGen: Gen[Length]     = Gen.posNum[Double].map(_ meters)
  final val durationGen: Gen[Duration] = Gen.posNum[Int].map(_ seconds)

  final val distanceGen: Gen[Distance] = for {
    length   <- lengthGen
    duration <- durationGen
  } yield Distance(length = length, duration = duration)

  final val latLongGen: Gen[LatLong] = for {
    lat: Double  <- Gen.posNum[Double]
    long: Double <- Gen.posNum[Double]
  } yield LatLong(latitude = lat, longitude = long)

  import enumeratum.scalacheck._

  final val travelModeGen: Gen[TravelMode] = implicitly[Arbitrary[TravelMode]].arbitrary

  final val directedPathGen: Gen[DirectedPathMultipleModes] = for {
    origin      <- latLongGen
    destination <- latLongGen
    travelModes <- Gen.nonEmptyListOf(travelModeGen)
  } yield
    DirectedPathMultipleModes(
      origin = origin,
      destination = destination,
      travelModes = travelModes
    )

  final val postalCodeGen: Gen[PostalCode] = Gen.listOfN(5, Gen.alphaNumChar).map(cs => PostalCode(cs.mkString))

  final val nonAmbigueAddressGen: Gen[NonAmbiguousAddress] = for {
    line1      <- Gen.alphaNumStr
    line2      <- Gen.alphaNumStr
    postalCode <- Gen.listOfN(5, Gen.alphaNumChar).map(_.mkString)
    town       <- Gen.alphaNumStr
    country    <- Gen.alphaNumStr
  } yield
    NonAmbiguousAddress(
      line1 = line1,
      line2 = line2,
      postalCode = postalCode,
      town = town,
      country = country
    )

  final val totoGen: Gen[Toto] = for {
    name     <- nameGen
    age      <- ageGen
    latLong  <- latLongGen
    distance <- distanceGen
  } yield
    Toto(
      name = name,
      age = age,
      latLong = latLong,
      distance = distance
    )
}
