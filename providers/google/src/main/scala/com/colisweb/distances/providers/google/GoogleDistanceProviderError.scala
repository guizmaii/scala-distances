package com.colisweb.distances.providers.google

import com.colisweb.distances.error.DistanceApiError

import scala.util.control.NoStackTrace

sealed trait GoogleDistanceProviderError extends DistanceApiError
final case class DistanceNotFound(message: String)
    extends Exception(message)
    with GoogleDistanceProviderError
    with NoStackTrace
final case class NoResults(message: String)
    extends Exception(message)
    with GoogleDistanceProviderError
    with NoStackTrace
final case class PastTraffic(message: String)
    extends Exception(message)
    with GoogleDistanceProviderError
    with NoStackTrace
final case class UnknownGoogleError(message: String) extends Exception(message) with GoogleDistanceProviderError
