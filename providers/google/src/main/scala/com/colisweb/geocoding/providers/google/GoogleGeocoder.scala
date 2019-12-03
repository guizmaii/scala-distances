package com.colisweb.geocoding.providers.google

import cats.effect.Concurrent
import cats.implicits._
import com.colisweb.distances.model.Point
import com.colisweb.geocoding.Geocoder
import com.colisweb.geocoding.Model.{NonAmbiguousAddress, PostalCode}
import com.colisweb.google.{CallbackEffect, GoogleGeoApiContext}
import com.google.maps.model.{ComponentFilter, GeocodingResult}
import com.google.maps.{GeocodingApi, GeocodingApiRequest}

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
