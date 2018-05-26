package com.guizmaii.distances

import cats.effect.Async
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

abstract class Cache[AIO[_]](ttl: Option[Duration])(implicit AIO: Async[AIO]) {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  private[distances] implicit val innerCache: InnerCache[Json]

  // TODO Jules: An optimization is possible when there's a cache miss because in that case the deserialization is useless.
  private[distances] def cachingF[V](keyParts: Any*)(f: => AIO[V])(
      implicit decoder: Decoder[V],
      encoder: Encoder[V]
  ): AIO[V] =
    innerCache
      .cachingF(keyParts: _*)(ttl)(f.map(encoder.apply))
      .flatMap(json => AIO.fromEither(decoder.decodeJson(json)))

}