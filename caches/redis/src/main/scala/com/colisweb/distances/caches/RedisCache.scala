package com.colisweb.distances.caches

import cats.effect.Async
import com.colisweb.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object RedisCache {

  import scalacache.redis.{RedisCache => InnerRedisCache}

  final def apply[F[_]: Async](config: RedisConfiguration, ttl: Option[Duration]): Cache[F] =
    new Cache[F](ttl) {
      import scalacache.serialization.circe._
      override private[distances] final val innerCache: InnerCache[Json] = InnerRedisCache[Json](config.jedisPool)
    }

}