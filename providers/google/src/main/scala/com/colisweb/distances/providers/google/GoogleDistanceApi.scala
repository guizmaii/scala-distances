package com.colisweb.distances.providers.google

import cats.MonadError
import cats.effect.Concurrent
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DepartureTime, DistanceAndDuration, Path, TravelModeTransportation}

class GoogleDistanceApi[F[_], P <: Path with TravelModeTransportation with DepartureTime](
    provider: GoogleDistanceProvider[F]
) extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    provider.singleRequest(path.travelMode, path.origin, path.destination, path.departureTime)
}

object GoogleDistanceApi {
  def sync[F[_], P <: Path with TravelModeTransportation with DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(
      implicit F: MonadError[F, Throwable]
  ): GoogleDistanceApi[F, P] = {
    val executor = new SyncRequestExecutor[F]
    val provider = new GoogleDistanceProvider(googleContext, trafficModel, executor)
    new GoogleDistanceApi[F, P](provider)
  }

  def async[F[_], P <: Path with TravelModeTransportation with DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(
      implicit F: Concurrent[F]
  ): GoogleDistanceApi[F, P] = {
    val executor = new AsyncRequestExecutor[F]
    val provider = new GoogleDistanceProvider(googleContext, trafficModel, executor)
    new GoogleDistanceApi[F, P](provider)
  }

}
