package com.colisweb.distances

import cats.Id
import com.colisweb.distances.model.DistanceAndDuration
import com.colisweb.distances.model.Path.PathSimple
import com.colisweb.distances.util.FromMapDistances
import com.colisweb.distances.util.TestTypes.{FirstError, SecondError}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class FallbackBatchSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {

  import builder.base._
  import com.colisweb.distances.generator.Generators._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 20, workers = 4)

  private val firstError  = FirstError
  private val secondError = SecondError

  private def verifyDistanceForAllPathsMatchExpected(
      distances: Distances.BuilderBatch[Id, SecondError.type, Unit],
      mapping: Map[PathSimple, DistanceAndDuration],
      slices: List[BatchSlice]
  ) = {
    val mappingAsArray = mapping.toArray
    slices
      .map {
        case BatchSlice(index, size) =>
          val batch    = mappingAsArray.slice(index, index + size)
          val paths    = batch.map { case (path, _) => path }.toList
          val expected = batch.toMap.mapValues(Right(_))
          distances.apply(paths) shouldBe expected
      }
  }

  private def verifyDistanceForAllPathsReturnsSecondError(
      distances: Distances.BuilderBatch[Id, SecondError.type, Unit],
      paths: List[PathSimple],
      slices: List[BatchSlice]
  ) = {
    val pathsAsArray = paths.toArray
    slices
      .map {
        case BatchSlice(index, size) =>
          val batch    = pathsAsArray.slice(index, index + size)
          val expected = batch.map(_ -> Left(SecondError)).toMap
          distances.apply(batch.toList) shouldBe expected
      }
  }

  "FallbackBatch" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping, slices) =>
      val first     = FromMapDistances[Id].batchFromMapOrError(firstError, mapping)
      val second    = FromMapDistances[Id].batchEmptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping, slices)
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping, slices) =>
      val first     = FromMapDistances[Id].batchEmptyAndError(firstError)
      val second    = FromMapDistances[Id].batchFromMapOrError(secondError, mapping)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping, slices)
    }

    "always return a value when first or second give a result" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping1, mapping2, slices) =>
      val first     = FromMapDistances[Id].batchFromMapOrError(firstError, mapping1)
      val second    = FromMapDistances[Id].batchFromMapOrError(secondError, mapping2)
      val distances = first.fallback(second)
      // ++ order matters : if a path is present in both, value will be from mapping1
      verifyDistanceForAllPathsMatchExpected(distances, mapping2 ++ mapping1, slices)
    }

    "always return second's error when first and second never give a result" in forAll(
      Gen.nonEmptyListOf(genPathSimple),
      genBatchSlices
    ) { (paths, slices) =>
      val first     = FromMapDistances[Id].batchEmptyAndError(firstError)
      val second    = FromMapDistances[Id].batchEmptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsReturnsSecondError(distances, paths, slices)
    }
  }

  "FallbackBatchOption" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping, slices) =>
      val first     = FromMapDistances[Id].batchFromMap(mapping)
      val second    = FromMapDistances[Id].batchEmptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping, slices)
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping, slices) =>
      val first     = FromMapDistances[Id].batchEmpty
      val second    = FromMapDistances[Id].batchFromMapOrError(secondError, mapping)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsMatchExpected(distances, mapping, slices)
    }

    "always return a value when first or second give a result" in forAll(
      genPathSimpleAndDistanceUnrelatedSet,
      genPathSimpleAndDistanceUnrelatedSet,
      genBatchSlices
    ) { (mapping1, mapping2, slices) =>
      val first     = FromMapDistances[Id].batchFromMap(mapping1)
      val second    = FromMapDistances[Id].batchFromMapOrError(secondError, mapping2)
      val distances = first.fallback(second)
      // ++ order matters : if a path is present in both, value will be from mapping1
      verifyDistanceForAllPathsMatchExpected(distances, mapping2 ++ mapping1, slices)
    }

    "always return second's error when first and second never give a result" in forAll(
      Gen.nonEmptyListOf(genPathSimple),
      genBatchSlices
    ) { (paths, slices) =>
      val first     = FromMapDistances[Id].batchEmptyAndError(firstError)
      val second    = FromMapDistances[Id].batchEmptyAndError(secondError)
      val distances = first.fallback(second)
      verifyDistanceForAllPathsReturnsSecondError(distances, paths, slices)
    }
  }
}
