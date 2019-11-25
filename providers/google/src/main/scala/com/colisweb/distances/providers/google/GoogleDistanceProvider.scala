package com.colisweb.distances.providers.google

import java.time.{Instant, LocalDateTime, ZoneOffset}

import cats.effect.Concurrent
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types.LatLong._
import com.colisweb.distances.Types.{Distance, LatLong, Segment, TrafficHandling}
import com.colisweb.distances.{DistanceProvider, TravelMode}
import com.google.maps.model.DistanceMatrixElementStatus._
import com.google.maps.model.{DistanceMatrix, DistanceMatrixElementStatus, Unit => GoogleDistanceUnit}
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}

import scala.concurrent.duration._
import scala.language.postfixOps

object GoogleDistanceProvider {

  import cats.implicits._
  import com.colisweb.distances.providers.google.utils.Implicits._
  import squants.space.LengthConversions._

  final def apply[F[_]: Concurrent](
      geoApiContext: GoogleGeoApiContext
  ): DistanceProvider[F, GoogleDistanceProviderError] =
    new DistanceProvider[F, GoogleDistanceProviderError] {

      def buildGoogleRequest(
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong]
      ): DistanceMatrixApiRequest =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(mode.asGoogle)
          .origins(origins.map(_.asGoogle): _*)
          .destinations(destinations.map(_.asGoogle): _*)
          .units(GoogleDistanceUnit.METRIC)

      def requestWithTraffic(request: DistanceMatrixApiRequest)(
          trafficHandling: TrafficHandling
      ): DistanceMatrixApiRequest =
        request
          .departureTime(trafficHandling.departureTime)
          .trafficModel(trafficHandling.trafficModel.asGoogle)

      def handleGoogleResponse(
          response: F[DistanceMatrix],
          mode: TravelMode,
          orderedPairs: List[Segment]
      ): F[Map[Segment, Either[GoogleDistanceProviderError, Distance]]] =
        response.map { distanceMatrix =>
          getDistances(distanceMatrix)
            .zip(orderedPairs)
            .map {
              case ((status, maybeDistance), segment) =>
                segment -> handleMatrixElementStatus(mode, status, maybeDistance, segment.origin, segment.destination)
            }
            .toMap
        }

      override final def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Either[GoogleDistanceProviderError, Distance]] =
        batchDistances(mode, List(origin), List(destination), maybeTrafficHandling).map(_.values.head)

      /**
        * Call the Google Maps API with the following arguments.
        * /!\ Using the maybeTrafficHandling argument results in a twice higher API call cost & traffic taken into account.
        *
        * @param mode Transportation mode (driving, bicycle...)
        * @param origins Origin points
        * @param destinations Destination points
        * @param maybeTrafficHandling The traffic parameters, which are the departure time and the traffic estimation model.
        *                             If defined, this makes Google Maps take the traffic into account.
        * @return An Async typeclass instance of [[Distance]]
        */
      override final def batchDistances(
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong],
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Map[Segment, Either[GoogleDistanceProviderError, Distance]]] = {
        val orderedSegments = origins.flatMap(origin => destinations.map(Segment(origin, _)))

        maybeTrafficHandling match {
          case Some(TrafficHandling(departureTime, trafficModel)) if departureTime.isBefore(Instant.now()) =>
            val map: Map[Segment, Either[GoogleDistanceProviderError, Distance]] =
              orderedSegments.map { segment =>
                segment ->
                  Left[GoogleDistanceProviderError, Distance](
                    PastTraffic(
                      s"""
                       | Google Distance API does not handle past traffic requests.
                       | At ${LocalDateTime
                           .ofInstant(departureTime, ZoneOffset.UTC)} with model $trafficModel from ${segment.origin.show} to ${segment.destination.show}.
                      """.stripMargin
                    )
                  )
              }.toMap

            map.pure[F]

          case _ =>
            val baseRequest        = buildGoogleRequest(mode, origins, destinations)
            val googleFinalRequest = maybeTrafficHandling.fold(baseRequest)(requestWithTraffic(baseRequest)).asEffect[F]

            handleGoogleResponse(googleFinalRequest, mode, orderedSegments)
        }
      }
    }

  private[this] def handleMatrixElementStatus(
      mode: TravelMode,
      status: DistanceMatrixElementStatus,
      maybeDistance: Option[Distance],
      origin: LatLong,
      destination: LatLong
  ): Either[GoogleDistanceProviderError, Distance] = {
    (status, maybeDistance) match {
      case (OK, Some(distance)) => Right(distance)
      case (NOT_FOUND, _) =>
        Left[GoogleDistanceProviderError, Distance](
          DistanceNotFound(
            s"""
               | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
               |
               | Indication from Google API code doc: "Indicates that the origin and/or destination of this pairing could not be geocoded."
              """.stripMargin
          )
        )

      case (ZERO_RESULTS, _) =>
        Left[GoogleDistanceProviderError, Distance](
          NoResults(
            s"""
               | Google Distance API have zero results for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
               |
               | Indication from Google API code doc: "Indicates that no route could be found between the origin and destination."
              """.stripMargin
          )
        )

      case (_, None) =>
        Left[GoogleDistanceProviderError, Distance](
          UnknownGoogleError(
            s"""
               | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
              """.stripMargin
          )
        )
    }
  }

  @inline
  private[this] final def getDistances(
      distanceMatrix: DistanceMatrix
  ): List[(DistanceMatrixElementStatus, Option[Distance])] =
    (for {
      row     <- distanceMatrix.rows
      element <- row.elements
    } yield {
      element.status match {
        case OK =>
          val durationInTraffic = Option(element.durationInTraffic).map(_.inSeconds seconds)
          val travelDuration    = element.duration.inSeconds seconds

          OK -> Some(
            Distance(
              length = element.distance.inMeters meters,
              duration = durationInTraffic.getOrElse(travelDuration)
            )
          )

        case e => e -> None
      }
    }).toList
}
