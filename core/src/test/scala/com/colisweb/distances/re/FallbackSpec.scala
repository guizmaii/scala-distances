package com.colisweb.distances.re
import cats.Id
import com.colisweb.distances.re.util.{FromMapDistances, TestTypes}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class FallbackSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  import builder.base._
  import com.colisweb.distances.re.generator.Generators._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 20, workers = 4)

  private val firstError  = TestTypes.FirstError
  private val secondError = TestTypes.SecondError

  "Fallback" should {

    "always return a value when first gives a result and second never" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].fromMapOrError(firstError, mapping)
      val second    = FromMapDistances[Id].emptyAndError(secondError)
      val distances = first.fallback(second)
      val paths     = mapping.keys.toList
      val expected  = mapping.values.toList.map(Right(_))
      paths.map(distances.apply) should contain theSameElementsInOrderAs expected
    }

    "always return a value when first never fives a result and second always" in forAll(
      genPathSimpleAndDistanceUnrelatedSet
    ) { mapping =>
      val first     = FromMapDistances[Id].emptyAndError(firstError)
      val second    = FromMapDistances[Id].fromMapOrError(secondError, mapping)
      val distances = first.fallback(second)
      val paths     = mapping.keys.toList
      val results   = mapping.values.toList.map(Right(_))
      paths.map(distances.apply) should contain theSameElementsInOrderAs results
    }

//    "always return a value when first or second give a result" in {
//
//    }
//
//    "never return a value when first and second never give a result" in {
//
//    }
//
//
//    "call second when first returns an explicit error" in {
//      val path12   = Path(p1, p2, pp)
//      val first    = FromMapDistances[Id].emptyAndError(firstError)
//      val second   = FromMapDistances[Id].fromMapOrError(secondError, path12 -> d12)
//      val distance = first.fallback(second).apply(path12)
//
//      distance shouldBe Right(d12)
//    }
//
//    "not call second when first returns a result" in {
//      val path12 = Path(p1, p2, pp)
//      val first  = FromMapDistances[Id].fromMapOrError(firstError, path12 -> d12)
//      val second = FromMapDistances[Id].emptyAndError(secondError)
//
//      val distance = first.fallback(second).apply(path12)
//      distance shouldBe Right(d12)
//    }
//
//    "return second's error when it also failed" in {
//      val path12 = Path(p1, p2, pp)
//      val first  = FromMapDistances[Id].emptyAndError(firstError)
//      val second = FromMapDistances[Id].emptyAndError(secondError)
//
//      val distance = first.fallback(second).apply(path12)
//      distance shouldBe Left(secondError)
//    }
//  }
//
//  "FallbackOptional" should {
//
//    "call second when first returns an explicit error" in {
//      val path12 = Path(p1, p2, pp)
//      val first  = FromMapDistances[Id].empty
//      val second = FromMapDistances[Id].fromMapOrError(secondError, path12 -> d12)
//
//      val distance = first.fallback(second).apply(path12)
//      distance shouldBe Right(d12)
//    }
//
//    "not call second when first returns a result" in {
//      val path12 = Path(p1, p2, pp)
//      val first  = FromMapDistances[Id].fromMap(path12 -> d12)
//      val second = FromMapDistances[Id].emptyAndError(secondError)
//
//      val distance = FallbackOption(first, second).apply(path12)
//      distance shouldBe Right(d12)
//    }
//
//    "return second's error when it also failed" in {
//      val path12 = Path(p1, p2, pp)
//      val first  = FromMapDistances[Id].empty
//      val second = FromMapDistances[Id].emptyAndError(secondError)
//
//      val distance = FallbackOption(first, second).apply(path12)
//      distance shouldBe Left(secondError)
//    }
  }
}
