package com.colisweb.geocoding

import com.colisweb.distances.model.Point
import com.colisweb.geocoding.Model.{NonAmbiguousAddress, PostalCode}

trait Geocoder[F[_]] {

  def geocode(postalCode: PostalCode): F[Option[Point]]

  def geocode(address: NonAmbiguousAddress): F[Option[Point]]
}
