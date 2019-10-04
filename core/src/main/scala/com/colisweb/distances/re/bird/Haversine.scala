package com.colisweb.distances.re.bird
import com.colisweb.distances.re.model.Point

object Haversine {

  def distance(origin: Point, destination: Point): Double = {
    import scala.math._

    val deltaLat = toRadians(destination.latitude - origin.latitude)
    val deltaLon = toRadians(destination.longitude - origin.longitude)

    val hav = pow(sin(deltaLat / 2), 2) + cos(toRadians(origin.latitude)) * cos(
      toRadians(destination.latitude)
    ) * pow(sin(deltaLon / 2), 2)
    val greatCircleDistance = 2 * atan2(sqrt(hav), sqrt(1 - hav))

    val earthRadiusMiles  = 3958.761
    val earthRadiusMeters = earthRadiusMiles / 0.00062137
    earthRadiusMeters * greatCircleDistance
  }
}
