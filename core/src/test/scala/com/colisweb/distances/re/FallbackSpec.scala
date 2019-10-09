package com.colisweb.distances.re
import cats.Id
import com.colisweb.distances.re.model.Path
import com.colisweb.distances.re.util.{StaticDistanceApi, StaticDistanceOptionApi, TestTypes, TestValues}
import org.scalatest.{Matchers, WordSpec}

class FallbackSpec extends WordSpec with Matchers {
  import TestValues._

  "Fallback" should {

    "call second when first returns an explicit error" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Left(TestTypes.ErrorBis("oh noes"))
      )
      val second = StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Right(d12)
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "not call second when first returns a result" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Right(d12)
      )
      val second = StaticDistanceApi.empty[Id, TestTypes.Error]

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "return second's error when it also failed" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Left(TestTypes.ErrorBis("oh noes"))
      )
      val second = StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Left(TestTypes.Error("boom"))
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Left(TestTypes.Error("boom"))
    }
  }

  "FallbackOptional" should {

    "call second when first returns an explicit error" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceOptionApi[Id](
        path12 -> None
      )
      val second = StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Right(d12)
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "not call second when first returns a result" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceOptionApi[Id](
        path12 -> Some(d12)
      )
      val second = StaticDistanceApi.empty[Id, TestTypes.Error]

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "return second's error when it also failed" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceOptionApi[Id](
        path12 -> None
      )
      val second = StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Left(TestTypes.Error("boom"))
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Left(TestTypes.Error("boom"))
    }
  }
}
