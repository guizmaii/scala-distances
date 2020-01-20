package com.colisweb.distances.utils

import cats.Monad
import cats.effect.Async
import com.colisweb.distances.Types.{DirectedPath, Distance, LatLong, Segment, TrafficHandling}
import com.colisweb.distances.caches.NoCache
import com.colisweb.distances.{DistanceProvider, _}
import squants.motion.KilometersPerHour
import squants.space.Meters

import scala.concurrent.duration._
import scala.math._

object Stubs {
  def distanceProviderStub[F[_]: Async, E]: DistanceProvider[F, E] =
    new DistanceProvider[F, E] {
      override def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Either[E, Distance]] = ???

      override def batchDistances(
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong],
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Map[Segment, Either[E, Distance]]] = ???
    }

  def geoProviderStub[F[_]: Async]: GeoProvider[F] = new GeoProvider[F] {
    override def geocode(point: Types.Point): F[LatLong] = ???
  }

  def geocoderStub[F[_]: Async] = new Geocoder[F](geoProviderStub, NoCache())

  private def haversine(origin: LatLong, destination: LatLong): Double = {
    val deltaLat = toRadians(destination.latitude - origin.latitude)
    val deltaLon = toRadians(destination.longitude - origin.longitude)

    val hav = pow(sin(deltaLat / 2), 2) + cos(toRadians(origin.latitude)) * cos(
      toRadians(destination.latitude)
    ) * pow(sin(deltaLon / 2), 2)
    val greatCircleDistance = 2 * atan2(sqrt(hav), sqrt(1 - hav))

    val earthRadiusMiles  = 3958.761
    val earthRadiusMeters = earthRadiusMiles / 0.00062137

    earthRadiusMeters * greatCircleDistance
  }

  def mockedCacheKey(path: DirectedPath): Seq[Any] =
    Seq(path.origin, path.destination, path.travelMode, path.maybeTrafficHandling)

  def mockedBatchDistanceF[F[_]: Monad](
      mode: TravelMode,
      origins: List[LatLong],
      destinations: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Map[Segment, Either[Unit, Distance]]] =
    Monad[F].pure {
      origins
        .flatMap(origin => destinations.map(Segment(origin, _)))
        .map { segment =>
          val either: Either[Unit, Distance] =
            Right[Unit, Distance](mockDistance(mode, segment.origin, segment.destination, maybeTrafficHandling))

          segment -> either
        }
        .toMap
    }

  def mockedBatchDistanceErrorF[F[_]: Async](
      mode: TravelMode,
      origins: List[LatLong],
      destinations: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Map[Segment, Either[Unit, Distance]]] =
    Monad[F].pure {
      // FIXME: does not compile if arguments are not used
      origins
        .flatMap(origin => destinations.map(destination => (Segment(origin, destination), mode, maybeTrafficHandling)))
        .map { case (segment, _, _) => segment -> Left[Unit, Distance](()) }
        .toMap
    }

  def mockedDistanceF[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Either[Unit, Distance]] =
    Monad[F].pure(Right(mockDistance(mode, origin, destination, maybeTrafficHandling)))

  def mockedDistanceErrorF[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Either[Unit, Distance]] =
    // FIXME: does not compile if arguments are not used
    Monad[F].pure(((mode, origin, destination, maybeTrafficHandling) -> Left(()))._2)

  private def mockDistance[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): Distance = {
    val distance       = Meters(haversine(origin, destination).round)
    val travelDuration = (distance / KilometersPerHour(50)).toSeconds.seconds

    val trafficDuration =
      mode match {
        case TravelMode.Driving =>
          maybeTrafficHandling.map { trafficHandling =>
            trafficHandling.trafficModel match {
              case TrafficModel.BestGuess   => 5.minutes
              case TrafficModel.Optimistic  => 2.minutes
              case TrafficModel.Pessimistic => 10.minutes
            }
          }

        case _ => None
      }

    Distance(distance, trafficDuration.getOrElse(travelDuration))
  }
}
