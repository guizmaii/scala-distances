package com.colisweb.google

import cats.effect.Concurrent
import com.google.maps.PendingResult

object CallbackEffect {

  implicit final class PendingResultOps[T](val request: PendingResult[T]) {
    def asEffect[F[_]: Concurrent]: F[T] =
      Concurrent[F].cancelable { cb =>
        request.setCallback(new PendingResult.Callback[T] {
          override def onResult(result: T): Unit     = cb(Right(result))
          override def onFailure(e: Throwable): Unit = cb(Left(e))
        })

        Concurrent[F].delay(request.cancel())
      }
  }
}
