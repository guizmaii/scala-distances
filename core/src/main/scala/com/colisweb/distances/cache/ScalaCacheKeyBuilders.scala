package com.colisweb.distances.cache

import ScalaCacheCache.Key
import com.colisweb.distances.model.Path._

object ScalaCacheKeyBuilders {

  implicit val pathSimple: Key[PathSimple]                       = Key.forProduct[PathSimple]
  implicit val pathVelocity: Key[PathVelocity]                   = Key.forProduct[PathVelocity]
  implicit val pathTravelMode: Key[PathTravelMode]               = Key.forProduct[PathTravelMode]
  implicit val pathTravelModeTraffic: Key[PathTravelModeTraffic] = Key.forProduct[PathTravelModeTraffic]
  implicit val pathTravelModeTrafficVelocity: Key[PathTravelModeTrafficVelocity] =
    Key.forProduct[PathTravelModeTrafficVelocity]
}
