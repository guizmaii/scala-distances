package com.colisweb.geocoding

object Model {

  final case class PostalCode(value: String) extends AnyVal

  final case class NonAmbiguousAddress(
      line1: String,
      line2: String,
      postalCode: String,
      town: String,
      country: String
  )
}
