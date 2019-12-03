package com.colisweb.distances.model

import Point.{Latitude, Longitude}

case class Point(latitude: Latitude, longitude: Longitude)

object Point {
  type Latitude  = Double
  type Longitude = Double
}
