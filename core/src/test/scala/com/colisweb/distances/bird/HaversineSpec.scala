package com.colisweb.distances.bird

import com.colisweb.distances.model.Point
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class HaversineSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  import com.colisweb.distances.generator.Boundaries._
  import com.colisweb.distances.generator.Generators._
  import com.colisweb.distances.generator.PointBoundaries._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100, workers = 4)

  "Haversine.distanceInKm" should {

    "with fixed latitude, increase with longitude, in half-globe boundary" in forAll(
      genLatitude(World),
      genThreeSuccessiveValuesInDirection(longitudeMin, longitudeMax)
    ) { (latitude, longitudes) =>
      val point   = Point(latitude, longitudes._1)
      val closer  = Point(latitude, longitudes._2)
      val farther = Point(latitude, longitudes._3)
      Haversine.distanceInKm(point, closer) should be <= Haversine.distanceInKm(point, farther)
    }

    "with fixed longitude, increase with latitude, in half-globe boundary" in forAll(
      genThreeSuccessiveValuesInDirection(latitudeMin, latitudeMax),
      genLongitude(World)
    ) { (latitudes, longitude) =>
      val point   = Point(latitudes._1, longitude)
      val closer  = Point(latitudes._2, longitude)
      val farther = Point(latitudes._3, longitude)
      Haversine.distanceInKm(point, closer) should be <= Haversine.distanceInKm(point, farther)
    }

    "be 0 for 2 identical points" in forAll(genPoint(World)) { point =>
      Haversine.distanceInKm(point, point) shouldBe 0.0d
    }

    "be the same for 2 points as origin or destination" in forAll(genPoint(World), genPoint(World)) {
      (point1, point2) =>
        Haversine.distanceInKm(point1, point2) shouldBe Haversine.distanceInKm(point2, point1)
    }
  }
}
