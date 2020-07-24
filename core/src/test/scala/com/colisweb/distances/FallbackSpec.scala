package com.colisweb.distances

import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, PathPlain}
import com.colisweb.distances.util.FromMapDistances
import com.colisweb.distances.util.TestTypes.{FirstError, SecondError}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success, Try}

class FallbackSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  import com.colisweb.distances.generator.Generators._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 20, workers = 4)

  private val firstError: FirstError   = FirstError
  private val secondError: SecondError = SecondError

  private def verifyDistanceForAllPathsMatchExpected(
      distances: DistanceApi[Try, PathPlain],
      mapping: Map[PathPlain, DistanceAndDuration]
  ) = {
    mapping.toList.map {
      case (path, expected) =>
        (path -> distances.distance(path)) shouldBe (path -> Success(expected))
    }
  }

  private def verifyDistanceForAllPathsReturnsSecondError(
      distances: DistanceApi[Try, PathPlain],
      paths: List[PathPlain]
  ) = {
    paths.map { path =>
      (path -> distances.distance(path)) shouldBe (path -> Failure(secondError))
    }
  }

  "Fallback" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Try].fromMapOrError(mapping, firstError)
      val second    = FromMapDistances[Try].emptyAndError(secondError)
      val distances = first.fallback(second).build
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Try].emptyAndError(firstError)
      val second    = FromMapDistances[Try].fromMapOrError(mapping, secondError)
      val distances = first.fallback(second).build
      verifyDistanceForAllPathsMatchExpected(distances, mapping)
    }

    "always return a value when first or second give a result" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genPathSimpleAndDistanceUnrelatedSet
    ) { (mapping1, mapping2) =>
      val first     = FromMapDistances[Try].fromMapOrError(mapping1, firstError)
      val second    = FromMapDistances[Try].fromMapOrError(mapping2, secondError)
      val distances = first.fallback(second).build
      // ++ order matters : if a path is present in both, value will be from mapping1
      verifyDistanceForAllPathsMatchExpected(distances, mapping2 ++ mapping1)
    }

    "always return second's error when first and second never give a result" in forAll(
      Gen.nonEmptyListOf(genPathBetween)
    ) { paths =>
      val first     = FromMapDistances[Try].emptyAndError(firstError)
      val second    = FromMapDistances[Try].emptyAndError(secondError)
      val distances = first.fallback(second).build
      verifyDistanceForAllPathsReturnsSecondError(distances, paths)
    }
  }
}
