package com.colisweb.distances

import cats.Id
import com.colisweb.distances.model.DistanceAndDuration
import com.colisweb.distances.model.Path.PathSimple
import com.colisweb.distances.util.FromMapDistances
import com.colisweb.distances.util.TestTypes.{FirstError, SecondError}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class FallbackSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  import builder.base._
  import com.colisweb.distances.generator.Generators._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 20, workers = 4)

  private val firstError  = FirstError
  private val secondError = SecondError

  private def verifyDistanceForAllPathsMatchExpected(
      distances: Distances.Builder[Id, SecondError.type, Unit],
      mapping: Map[PathSimple, DistanceAndDuration]
  ) = {
    mapping.toList.map {
      case (path, expected) =>
        (path -> distances.apply(path)) shouldBe (path -> Right(expected))
    }
  }

  private def verifyDistanceForAllPathsReturnsSecondError(
      distances: Distances.Builder[Id, SecondError.type, Unit],
      paths: List[PathSimple]
  ) = {
    paths.map { path =>
      (path -> distances.apply(path)) shouldBe (path -> Left(secondError))
    }
  }

  "Fallback" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].fromMapOrError(firstError, mapping)
      val second    = FromMapDistances[Id].emptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].emptyAndError(firstError)
      val second    = FromMapDistances[Id].fromMapOrError(secondError, mapping)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first or second give a result" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genPathSimpleAndDistanceUnrelatedSet
    ) { (mapping1, mapping2) =>
      val first     = FromMapDistances[Id].fromMapOrError(firstError, mapping1)
      val second    = FromMapDistances[Id].fromMapOrError(secondError, mapping2)
      val distances = first.fallback(second)
      // ++ order matters : if a path is present in both, value will be from mapping1
      verifyDistanceForAllPathsMatchExpected(distances, mapping2 ++ mapping1)
    }

    "always return second's error when first and second never give a result" in forAll(
      Gen.nonEmptyListOf(genPathSimple)
    ) { paths =>
      val first     = FromMapDistances[Id].emptyAndError(firstError)
      val second    = FromMapDistances[Id].emptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsReturnsSecondError(distances, paths)
    }
  }

  "FallbackOptional" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].fromMap(mapping)
      val second    = FromMapDistances[Id].emptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].empty
      val second    = FromMapDistances[Id].fromMapOrError(secondError, mapping)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first or second give a result" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genPathSimpleAndDistanceUnrelatedSet
    ) { (mapping1, mapping2) =>
      val first     = FromMapDistances[Id].fromMap(mapping1)
      val second    = FromMapDistances[Id].fromMapOrError(secondError, mapping2)
      val distances = first.fallback(second)
      // ++ order matters : if a path is present in both, value will be from mapping1
      verifyDistanceForAllPathsMatchExpected(distances, mapping2 ++ mapping1)
    }

    "always return second's error when first and second never give a result" in forAll(
      Gen.nonEmptyListOf(genPathSimple)
    ) { paths =>
      val first     = FromMapDistances[Id].emptyAndError(firstError)
      val second    = FromMapDistances[Id].emptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsReturnsSecondError(distances, paths)
    }
  }
}
