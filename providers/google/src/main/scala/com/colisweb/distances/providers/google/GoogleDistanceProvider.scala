package com.colisweb.distances.providers.google

import cats.data.Kleisli
import com.colisweb.distances.Distances
import com.colisweb.distances.model.Path.{DepartureTimeParameter, TravelModeParameter}
import com.colisweb.distances.model.{Path, TravelMode}

class GoogleDistanceProvider[F[_]](api: GoogleDistanceApi[F]) {

  def builder[R: TravelModeParameter]: Distances.Builder[F, GoogleDistanceProviderError, R] = Kleisli { path: Path[R] =>
    api.singleRequest(TravelModeParameter.extract(path), path.origin, path.destination, None)
  }

  def builderWithTraffic[R: TravelModeParameter: DepartureTimeParameter]
    : Distances.Builder[F, GoogleDistanceProviderError, R] = Kleisli { path: Path[R] =>
    api.singleRequest(
      TravelModeParameter.extract(path),
      path.origin,
      path.destination,
      DepartureTimeParameter.extract(path)
    )
  }
}

class GoogleDistanceProviderForTravelMode[F[_]](api: GoogleDistanceApi[F], travelMode: TravelMode) {

  def builder[R]: Distances.Builder[F, GoogleDistanceProviderError, R] = Kleisli { path: Path[R] =>
    api.singleRequest(travelMode, path.origin, path.destination, None)
  }

  def builderWithTraffic[R: DepartureTimeParameter]: Distances.Builder[F, GoogleDistanceProviderError, R] = Kleisli {
    path: Path[R] =>
      api.singleRequest(travelMode, path.origin, path.destination, DepartureTimeParameter.extract(path))
  }
}

object GoogleDistanceProvider {

  def apply[F[_]](api: GoogleDistanceApi[F]): GoogleDistanceProvider[F] =
    new GoogleDistanceProvider[F](api)

  def forTravelMode[F[_]](api: GoogleDistanceApi[F], travelMode: TravelMode): GoogleDistanceProviderForTravelMode[F] =
    new GoogleDistanceProviderForTravelMode[F](api, travelMode)
}
