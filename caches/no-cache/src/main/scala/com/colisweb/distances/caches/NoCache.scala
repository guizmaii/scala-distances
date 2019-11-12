package com.colisweb.distances.caches

import cats.implicits._
import cats.effect.Async
import com.colisweb.distances.Cache
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

object NoCache {

  final def apply[F[_]: Async](): Cache[F] =
    new Cache[F](None) {
      override private[distances] final val innerCache: InnerCache[Json] = null

      @inline
      override def cachingF[V](f: F[V], decoder: Decoder[V], encoder: Encoder[V], keys: Any*): F[V] = f

      @inline
      override def caching[V](v: V, decoder: Decoder[V], encoder: Encoder[V], keys: Any*): F[V] = v.pure[F]

      @inline
      override def get[V](decoder: Decoder[V], keys: Any*): F[Option[V]] = Option.empty[V].pure[F]

      @inline
      override def remove(keys: Any*): F[Unit] = ().pure[F]

      @inline
      override def removeAll(): F[Unit] = ().pure[F]
    }

}
