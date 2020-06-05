package com.colisweb.distances

import cats.effect.Async
import com.colisweb.distances.Types.{LatLong, NonAmbiguousAddress, PostalCode}

class Geocoder[F[_]: Async](provider: GeoProvider[F], cache: Cache[F]) {

  final def geocodePostalCode(postalCode: PostalCode): F[LatLong] =
    cache.cachingF(provider.geocode(postalCode), LatLong.decoder, LatLong.encoder, Seq(postalCode))

  final def geocodeNonAmbiguousAddress(address: NonAmbiguousAddress): F[LatLong] =
    cache.cachingF(provider.geocode(address), LatLong.decoder, LatLong.encoder, Seq(address))

}

object Geocoder {
  final def apply[F[_]: Async](provider: GeoProvider[F], cacheProvider: Cache[F]): Geocoder[F] =
    new Geocoder(provider, cacheProvider)
}
