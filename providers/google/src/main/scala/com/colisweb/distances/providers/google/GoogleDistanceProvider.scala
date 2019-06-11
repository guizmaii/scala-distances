package com.colisweb.distances.providers.google

import java.time.Instant

import cats.effect.{Concurrent, Sync}
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}
import com.google.maps.model.DistanceMatrixElementStatus._
import com.google.maps.model.TravelMode._
import com.google.maps.model.{
  DistanceMatrix,
  DistanceMatrixElementStatus,
  LatLng => GoogleLatLng,
  TravelMode => GoogleTravelMode,
  Unit => GoogleDistanceUnit
}
import com.colisweb.distances.DistanceProvider
import com.colisweb.distances.Types.TravelMode._
import com.colisweb.distances.Types.{Distance, LatLong, TravelMode}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NoStackTrace

sealed abstract class GoogleDistanceProviderError(message: String) extends RuntimeException(message) with NoStackTrace
final case class DistanceNotFound(message: String)                 extends GoogleDistanceProviderError(message)

object GoogleDistanceProvider {

  import cats.implicits._
  import com.colisweb.distances.providers.google.utils.Implicits._
  import squants.space.LengthConversions._

  final def apply[F[_]: Concurrent](geoApiContext: GoogleGeoApiContext): DistanceProvider[F] =
    new DistanceProvider[F] {

      def buildGoogleRequest(mode: TravelMode, origin: LatLong, destination: LatLong): DistanceMatrixApiRequest =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(asGoogleTravelMode(mode))
          .origins(asGoogleLatLng(origin)) // TODO: Multiple origins?
          .destinations(asGoogleLatLng(destination))
          .units(GoogleDistanceUnit.METRIC)

      def handleGoogleResponse(response: F[DistanceMatrix], mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance] =
        response
          .flatMap { response =>
            getDistance(response) match {
              case Some((OK, distance)) => distance.pure[F]
              case Some((NOT_FOUND, _)) =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                       |
                         | Indication from Google API code doc: "Indicates that the origin and/or destination of this pairing could not be geocoded."
                      """.stripMargin
                  )
                }
              case Some((ZERO_RESULTS, _)) =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API have zero results for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                       |
                         | Indication from Google API code doc: "Indicates that no route could be found between the origin and destination."
                      """.stripMargin
                  )
                }
              case None =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                      """.stripMargin
                  )
                }
            }
          }

      override final def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeDepartureTime: Option[Instant] = None
      ): F[Distance] = {
        val baseRequest        = buildGoogleRequest(mode, origin, destination)
        val googleFinalRequest = maybeDepartureTime.fold(baseRequest)(baseRequest.departureTime).asEffect[F]

        handleGoogleResponse(googleFinalRequest, mode, origin, destination)
      }
    }

  @inline
  private[this] final def asGoogleTravelMode(travelMode: TravelMode): GoogleTravelMode =
    travelMode match {
      case Driving   => DRIVING
      case Bicycling => BICYCLING
      case Walking   => WALKING
      case Transit   => TRANSIT
      case Unknown   => UNKNOWN
    }

  @inline
  private[this] final def getDistance(distanceMatrix: DistanceMatrix): Option[(DistanceMatrixElementStatus, Distance)] =
    for {
      row     <- distanceMatrix.rows.headOption
      element <- row.elements.headOption
    } yield
      element.status match {
        case OK => OK -> Distance(length = element.distance.inMeters meters, duration = element.duration.inSeconds seconds)
        case e  => e  -> null // Very bad !
      }

  @inline
  private[this] final def asGoogleLatLng(latLong: LatLong): GoogleLatLng = new GoogleLatLng(latLong.latitude, latLong.longitude)

}