package com.colisweb.distances.re
import cats.Id
import com.colisweb.distances.re.model.Path
import com.colisweb.distances.re.util.{FromMapDistances, TestTypes, TestValues}
import org.scalatest.{Matchers, WordSpec}

class FallbackSpec extends WordSpec with Matchers {
  import TestValues._
  import builder.base._

  private val firstError  = TestTypes.FirstError("oh noes")
  private val secondError = TestTypes.SecondError("boom")

  "Fallback" should {

    "call second when first returns an explicit error" in {
      val path12   = Path(p1, p2, pp)
      val first    = FromMapDistances[Id].emptyAndError(firstError)
      val second   = FromMapDistances[Id].fromMapOrError(secondError, path12 -> d12)
      val distance = first.fallback(second).apply(path12)

      distance shouldBe Right(d12)
    }

    "not call second when first returns a result" in {
      val path12 = Path(p1, p2, pp)
      val first  = FromMapDistances[Id].fromMapOrError(firstError, path12 -> d12)
      val second = FromMapDistances[Id].emptyAndError(secondError)

      val distance = first.fallback(second).apply(path12)
      distance shouldBe Right(d12)
    }

    "return second's error when it also failed" in {
      val path12 = Path(p1, p2, pp)
      val first  = FromMapDistances[Id].emptyAndError(firstError)
      val second = FromMapDistances[Id].emptyAndError(secondError)

      val distance = first.fallback(second).apply(path12)
      distance shouldBe Left(secondError)
    }
  }

  "FallbackOptional" should {

    "call second when first returns an explicit error" in {
      val path12 = Path(p1, p2, pp)
      val first  = FromMapDistances[Id].empty
      val second = FromMapDistances[Id].fromMapOrError(secondError, path12 -> d12)

      val distance = first.fallback(second).apply(path12)
      distance shouldBe Right(d12)
    }

    "not call second when first returns a result" in {
      val path12 = Path(p1, p2, pp)
      val first  = FromMapDistances[Id].fromMap(path12 -> d12)
      val second = FromMapDistances[Id].emptyAndError(secondError)

      val distance = FallbackOption(first, second).apply(path12)
      distance shouldBe Right(d12)
    }

    "return second's error when it also failed" in {
      val path12 = Path(p1, p2, pp)
      val first  = FromMapDistances[Id].empty
      val second = FromMapDistances[Id].emptyAndError(secondError)

      val distance = FallbackOption(first, second).apply(path12)
      distance shouldBe Left(secondError)
    }
  }
}
