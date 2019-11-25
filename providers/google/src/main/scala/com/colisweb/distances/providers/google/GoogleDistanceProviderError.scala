package com.colisweb.distances.providers.google

import scala.util.control.NoStackTrace

sealed abstract class GoogleDistanceProviderError(message: String) extends RuntimeException(message) with NoStackTrace
final case class DistanceNotFound(message: String)                 extends GoogleDistanceProviderError(message)
final case class NoResults(message: String)                        extends GoogleDistanceProviderError(message)
final case class PastTraffic(message: String)                      extends GoogleDistanceProviderError(message)
final case class UnknownGoogleError(message: String)               extends GoogleDistanceProviderError(message)
