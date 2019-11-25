package com.colisweb.distances.utils

import cats.kernel.Semigroup
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types.{DirectedPathMultipleModes, LatLong}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.breakOut

class TraversableLikeOpsSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterEach
    with PrivateMethodTester {

  "RichList#combineDuplicatesOn" should {

    import Implicits._

    val directedPathSemiGroup: PrivateMethod[Semigroup[DirectedPathMultipleModes]] =
      PrivateMethod[Semigroup[DirectedPathMultipleModes]]('directedPathSemiGroup)

    "remove duplicates and combine using the semigroup" in {
      val a00 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a01 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a11 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Bicycling :: Nil)
      val a21 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a22 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a23 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a24 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)

      val res = List(a00, a01, a11, a21, a22, a23, a24).combineDuplicatesOn {
        case DirectedPathMultipleModes(o, d, _, _) => (o, d)
      }(
        DistanceApi invokePrivate directedPathSemiGroup(),
        breakOut
      )

      res.head shouldBe DirectedPathMultipleModes(
        LatLong(42.0, 42.0),
        LatLong(43.0, 43.0),
        Driving :: Bicycling :: Unknown :: Nil
      )
    }
    "remove duplicates and combine using the semigroup, BIS" in {
      val a00 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a01 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a11 = DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Bicycling :: Nil)
      val b00 = DirectedPathMultipleModes(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Nil)
      val b01 = DirectedPathMultipleModes(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Nil)
      val b02 = DirectedPathMultipleModes(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Unknown :: Nil)
      val res = wrapRefArray(Array(a00, a01, a11, b00, b01, b02)).combineDuplicatesOn {
        case DirectedPathMultipleModes(o, d, _, _) => (o, d)
      }(
        DistanceApi invokePrivate directedPathSemiGroup(),
        breakOut
      )

      res shouldBe Vector(
        DirectedPathMultipleModes(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Unknown :: Nil),
        DirectedPathMultipleModes(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Bicycling :: Nil)
      )
    }
  }

}
