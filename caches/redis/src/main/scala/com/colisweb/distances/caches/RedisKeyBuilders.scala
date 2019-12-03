package com.colisweb.distances.caches

import com.colisweb.distances.caches.RedisCache.KeyBuilder
import com.colisweb.distances.re.model.Path._

object RedisKeyBuilders {

  implicit val pathSimple: KeyBuilder[PathSimple]         = RedisCache.productKeyBuilder[PathSimple]
  implicit val pathVelocity: KeyBuilder[PathVelocity]     = RedisCache.productKeyBuilder[PathVelocity]
  implicit val pathTravelMode: KeyBuilder[PathTravelMode] = RedisCache.productKeyBuilder[PathTravelMode]
  implicit val pathTravelModeTraffic: KeyBuilder[PathTravelModeTraffic] =
    RedisCache.productKeyBuilder[PathTravelModeTraffic]
  implicit val pathTravelModeTrafficVelocity: KeyBuilder[PathTravelModeTrafficVelocity] =
    RedisCache.productKeyBuilder[PathTravelModeTrafficVelocity]
}
