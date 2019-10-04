package com.colisweb.distances.re
import cats.Id
import com.colisweb.distances.re.model.{DistanceAndDuration, Path, Point}
import com.colisweb.distances.re.util.{StaticDistanceApi, TestTypes}
import org.scalatest.{Matchers, WordSpec}
import squants.space.Kilometers

import scala.concurrent.duration._

class FallbackSpec extends WordSpec with Matchers {

  private val p1  = Point(11.1, 22.2)
  private val p2  = Point(33.3, 44.4)
  private val pp  = TestTypes.IdParam("a")
  private val d12 = DistanceAndDuration(Kilometers(10), 11.seconds)

  "Fallback" should {

    "call second when first returns an explicit error" in {
      val path12 = Path(p1, p2, pp)
      val first = StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Left(TestTypes.ErrorBis("oh noes"))
      )
      val second = util.StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Right(d12)
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "not call second when first returns a result" in {
      val path12 = Path(p1, p2, pp)
      val first = util.StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Right(d12)
      )
      val second = StaticDistanceApi.empty[Id, TestTypes.Error]

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Right(d12)
    }

    "return second's error when it also failed" in {
      val path12 = Path(p1, p2, pp)
      val first = util.StaticDistanceApi[Id, TestTypes.ErrorBis](
        path12 -> Left(TestTypes.ErrorBis("oh noes"))
      )
      val second = util.StaticDistanceApi[Id, TestTypes.Error](
        path12 -> Left(TestTypes.Error("boom"))
      )

      val distance = Fallback(first, second).distance(path12)
      distance shouldBe Left(TestTypes.Error("boom"))
    }
  }
}
