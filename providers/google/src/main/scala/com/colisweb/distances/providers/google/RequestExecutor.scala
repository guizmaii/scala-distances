package com.colisweb.distances.providers.google

import cats.MonadError
import cats.effect.Concurrent
import com.google.maps.PendingResult

sealed trait RequestExecutor[F[_]] {
  def run[T](request: PendingResult[T]): F[T]
}

class SyncRequestExecutor[F[_]](implicit F: MonadError[F, Throwable]) extends RequestExecutor[F] {
  override def run[T](request: PendingResult[T]): F[T] = F.catchNonFatal(request.await())
}

class AsyncRequestExecutor[F[_]](implicit F: Concurrent[F]) extends RequestExecutor[F] {
  override def run[T](request: PendingResult[T]): F[T] = F.cancelable { cb =>
    request.setCallback(new PendingResult.Callback[T] {
      override def onResult(result: T): Unit     = cb(Right(result))
      override def onFailure(e: Throwable): Unit = cb(Left(e))
    })

    F.delay(request.cancel())
  }
}
