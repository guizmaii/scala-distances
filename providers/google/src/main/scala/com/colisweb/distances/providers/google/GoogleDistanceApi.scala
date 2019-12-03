package com.colisweb.distances.providers.google

import java.time.Instant

import cats.effect.Concurrent
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, Point, TrafficModel, TravelMode}
import com.colisweb.distances.providers.google.GoogleDistanceApi.RequestBuilder
import com.colisweb.google.{CallbackEffect, GoogleGeoApiContext}
import com.google.maps.model.{
  DistanceMatrix,
  DistanceMatrixElement,
  DistanceMatrixElementStatus,
  Unit => GoogleDistanceUnit
}
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}
import squants.space.LengthConversions._

import scala.concurrent.duration._

class GoogleDistanceApi[F[_]: Concurrent](googleContext: GoogleGeoApiContext, trafficModel: TrafficModel) {
  import CallbackEffect.PendingResultOps
  import RequestBuilder._

  def singleRequest(
      travelMode: TravelMode,
      origin: Point,
      destination: Point,
      departureTime: Option[Instant]
  ): F[Either[GoogleDistanceProviderError, DistanceAndDuration]] = {
    val request                 = RequestBuilder(googleContext, travelMode).withOriginDestination(origin, destination)
    val requestMaybeWithTraffic = departureTime.fold(request)(request.withTraffic(trafficModel, _)) // FIXME: not checking Past DepartureTime, needed ?
    requestMaybeWithTraffic
      .asEffect[F]
      .map(extractSingleResponse)
  }

  def matrixRequest(
      travelMode: TravelMode,
      origins: List[Point],
      destinations: List[Point],
      departureTime: Option[Instant]
  ): F[Map[Point, Map[Point, Either[GoogleDistanceProviderError, DistanceAndDuration]]]] = {
    val request                 = RequestBuilder(googleContext, travelMode).withOriginDestinationMatrix(origins, destinations)
    val requestMaybeWithTraffic = departureTime.fold(request)(request.withTraffic(trafficModel, _)) // FIXME: not checking Past DepartureTime, needed ?
    requestMaybeWithTraffic
      .asEffect[F]
      .map(extractBatchResponse(origins, destinations, _))
  }

  private def extractSingleResponse(
      matrix: DistanceMatrix
  ): Either[GoogleDistanceProviderError, DistanceAndDuration] = {
    val element = matrix.rows(0).elements(0)
    extractMatrixResponseElement(element)
  }

  private def extractBatchResponse[R](
      origins: List[Point],
      destinations: List[Point],
      matrix: DistanceMatrix
  ): Map[Point, Map[Point, Either[GoogleDistanceProviderError, DistanceAndDuration]]] = {
    matrix.rows
      .zip(origins)
      .map {
        case (row, origin) =>
          origin -> row.elements
            .zip(destinations)
            .map {
              case (element, destination) =>
                destination -> extractMatrixResponseElement(element)
            }
            .toMap
      }
      .toMap
  }

  private def extractMatrixResponseElement(
      element: DistanceMatrixElement
  ): Either[GoogleDistanceProviderError, DistanceAndDuration] =
    element.status match {
      case DistanceMatrixElementStatus.OK =>
        val durationInSeconds = element.duration.inSeconds
        val durationInTraffic = Option(element.durationInTraffic).fold(0L)(_.inSeconds)
        val totalDuration     = (durationInSeconds + durationInTraffic).seconds
        val distanceInMeters  = element.distance.inMeters.meters
        Right(DistanceAndDuration(distanceInMeters, totalDuration))

      case DistanceMatrixElementStatus.NOT_FOUND =>
        Left(DistanceNotFound("origin and/or destination of this pairing could not be geocoded"))

      case DistanceMatrixElementStatus.ZERO_RESULTS =>
        Left(NoResults("no route could be found between the origin and destination"))

      case _ =>
        Left(UnknownGoogleError(element.toString))
    }
}

object GoogleDistanceApi {

  object RequestBuilder {
    import GoogleModel._

    def apply(googleContext: GoogleGeoApiContext, travelMode: TravelMode): DistanceMatrixApiRequest =
      DistanceMatrixApi
        .newRequest(googleContext.geoApiContext)
        .mode(travelMode.asGoogle)
        .units(GoogleDistanceUnit.METRIC)

    implicit class RequestBatch(request: DistanceMatrixApiRequest) {
      def withOriginDestinationMatrix(origins: List[Point], destinations: List[Point]): DistanceMatrixApiRequest =
        request
          .origins(origins.map(_.asGoogle): _*)
          .destinations(destinations.map(_.asGoogle): _*)
    }

    implicit class RequestSingle(request: DistanceMatrixApiRequest) {
      def withOriginDestination(origin: Point, destination: Point): DistanceMatrixApiRequest =
        request
          .origins(origin.asGoogle)
          .destinations(destination.asGoogle)
    }

    implicit class RequestTraffic(request: DistanceMatrixApiRequest) {
      def withTraffic(trafficModel: TrafficModel, departureTime: Instant): DistanceMatrixApiRequest =
        request
          .trafficModel(trafficModel.asGoogle)
          .departureTime(departureTime)
    }
  }
}
