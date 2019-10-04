package com.colisweb.distances.re.model
import com.colisweb.distances.re.model.Point.{Latitude, Longitude}

case class Point(latitude: Latitude, longitude: Longitude)

object Point {
  type Latitude  = Double
  type Longitude = Double
}
