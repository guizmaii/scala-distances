package com.colisweb.distances

import cats.effect.Async
import com.colisweb.distances.Types._

// The errors embedded by E are related to the distance computation (e.g. no routes available)
// The errors embedded by F are on the technical side (e.g. timeout)
abstract class DistanceProvider[F[_]: Async, E] {
  def distance(
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Either[E, Distance]]

  /**
    * A provider implementing this method will try to compute the distance matrix between each origin and
    * each destination in a single call.
    */
  def batchDistances(
      mode: TravelMode,
      origins: List[LatLong],
      destinations: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[Segment, Either[E, Distance]]]
}

object DistanceProvider {
  type DistanceF[F[_], E] = (TravelMode, LatLong, LatLong, Option[TrafficHandling]) => F[Either[E, Distance]]

  type BatchDistanceF[F[_], E] =
    (TravelMode, List[LatLong], List[LatLong], Option[TrafficHandling]) => F[Map[Segment, Either[E, Distance]]]
}
