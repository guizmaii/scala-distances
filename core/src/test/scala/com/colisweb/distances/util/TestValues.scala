package com.colisweb.distances.util

import com.colisweb.distances.model.{DistanceAndDuration, Point}
import squants.space.Kilometers

import scala.concurrent.duration._

object TestValues {

  val p1 = Point(11.1, 22.2)
  val p2 = Point(33.3, 44.4)

  val pp = TestTypes.IdParam("a")

  val d12 = DistanceAndDuration(Kilometers(10), 11.seconds)
}
