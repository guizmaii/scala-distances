package com.colisweb.geocoding.providers

import cats.implicits._
import cats.effect.Concurrent
import com.colisweb.distances.re.model.Point
import com.colisweb.geocoding.Geocoder
import com.colisweb.geocoding.Model.{NonAmbiguousAddress, PostalCode}
import com.colisweb.google.GoogleGeoApiContext
import com.google.maps.model.{ComponentFilter, GeocodingResult}
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.colisweb.google.CallbackEffect

class GoogleGeocoder[F[_]: Concurrent](context: GoogleGeoApiContext) extends Geocoder[F] {
  import CallbackEffect.PendingResultOps

  override def geocode(postalCode: PostalCode): F[Option[Point]] =
    baseRequest
      .components(ComponentFilter.postalCode(postalCode.value))
      .asEffect[F]
      .map(extractResponse)

  override def geocode(address: NonAmbiguousAddress): F[Option[Point]] =
    baseRequest
      .components(ComponentFilter.country(address.country))
      .components(ComponentFilter.postalCode(address.postalCode))
      .address(s"${address.line1} ${address.line2} ${address.town}")
      .asEffect[F]
      .map(extractResponse)

  private def baseRequest: GeocodingApiRequest =
    GeocodingApi
      .newRequest(context.geoApiContext)
      .region("eu")
      .language("fr")

  private def extractResponse(results: Array[GeocodingResult]): Option[Point] =
    results.headOption
      .map { result =>
        val location = result.geometry.location
        Point(latitude = location.lat, longitude = location.lng)
      }
}
