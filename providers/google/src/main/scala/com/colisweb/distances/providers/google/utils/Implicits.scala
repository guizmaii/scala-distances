package com.colisweb.distances.providers.google.utils

import cats.{MonadError, Parallel}
import cats.effect.Concurrent
import com.colisweb.distances.{TrafficModel, TravelMode}
import com.colisweb.distances.TrafficModel.{BestGuess, Optimistic, Pessimistic}
import com.colisweb.distances.TravelMode.{Bicycling, Driving, Transit, Unknown, Walking}
import com.colisweb.distances.Types.LatLong
import com.google.maps.PendingResult
import com.google.maps.model.TrafficModel.{BEST_GUESS, OPTIMISTIC, PESSIMISTIC}
import com.google.maps.model.TravelMode.{BICYCLING, DRIVING, TRANSIT, UNKNOWN, WALKING}
import com.google.maps.model.{
  LatLng => GoogleLatLong,
  TrafficModel => GoogleTrafficModel,
  TravelMode => GoogleTravelMode
}

private[google] object Implicits {

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

  implicit final class Tuple3AsyncOps[F[_], A](val instance: (F[A], F[A], F[A])) {
    import cats.implicits._

    /**
      * Launch the 3 Tasks side effects in parallele but returns the result in order:
      *
      * If the first one succeed, it returns its result
      * else if the second one succeed, it returns its result
      * else if the last one succeed, it returns its result
      * else return the error of last one.
      *
      */
    def raceInOrder3(implicit F: MonadError[F, Throwable], par: Parallel[F]): F[A] = {
      val (a, b, c) = instance
      (a.attempt, b.attempt, c.attempt).parTupled
        .flatMap {
          case (Right(v), _, _)             => F.pure(v)
          case (Left(_), Right(v), _)       => F.pure(v)
          case (Left(_), Left(_), Right(v)) => F.pure(v)
          case (Left(_), Left(_), Left(e))  => F.raiseError(e)
        }
    }
  }

  implicit final class GoogleTrafficModelOps(model: TrafficModel) {
    def asGoogle: GoogleTrafficModel = model match {
      case BestGuess   => BEST_GUESS
      case Optimistic  => OPTIMISTIC
      case Pessimistic => PESSIMISTIC
    }
  }

  implicit final class GoogleTravelModeOps(mode: TravelMode) {
    def asGoogle: GoogleTravelMode = mode match {
      case Driving   => DRIVING
      case Bicycling => BICYCLING
      case Walking   => WALKING
      case Transit   => TRANSIT
      case Unknown   => UNKNOWN
    }
  }

  implicit final class GoogleLatLongOps(latLong: LatLong) {
    def asGoogle: GoogleLatLong = new GoogleLatLong(latLong.latitude, latLong.longitude)
  }
}
