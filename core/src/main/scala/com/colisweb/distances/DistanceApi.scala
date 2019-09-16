package com.colisweb.distances

import cats.Parallel
import cats.effect.Async
import cats.kernel.Semigroup
import com.colisweb.distances.Cache.{Caching, GetCached}
import com.colisweb.distances.DistanceProvider.{BatchDistanceF, DistanceF}
import com.colisweb.distances.Types._
import io.circe.{Decoder, Encoder}

import scala.collection.breakOut

/**
  * Main distance calls entry point.
  * @param distanceF A distance function which returns either the distance or an error wrapped in a F typeclass instance.
  *                  It won't get called if the distance is already cached.
  * @param batchDistanceF A distance function which make distances calls by batch of (origins, destinations) pairs.
  *                       It won't compute distances for already cached distances.
  *                       This can result in multiple calls in case there are cached distances.
  * @param caching A function which, given a value, caches it and returns it wrapped in a F typeclass instance.
  * @param getCached A function which try to retrieve a value from the cache. The value is wrapped in a Option,
  *                  which is also wrapped in a F typeclass instance.
  * @tparam F A typeclass which is constructed from Async and Parallel.
  * @tparam E An error type, specific to the distance provider.
  */
class DistanceApi[F[_]: Async: Parallel, E](
    distanceF: DistanceF[F, E],
    batchDistanceF: BatchDistanceF[F, E],
    caching: Caching[F, Distance],
    getCached: GetCached[F, Distance]
) {

  import DistanceApi._
  import cats.implicits._
  import com.colisweb.distances.utils.Implicits._

  final def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[TravelMode, Either[E, Distance]]] =
    if (origin == destination)
      Async[F].pure(travelModes.map(_ -> Right(Distance.zero))(breakOut))
    else
      parDistances(travelModes, origin, destination, maybeTrafficHandling)
        .map { distancesOrErrors =>
          distancesOrErrors.map { case (path, distanceOrError) => path.travelMode -> distanceOrError }.toMap
        }

  /**
    * Make distances computation by batch of points.
    * The computed distances are the combinations of origins and destinations arguments.
    * Only the uncached distances are computed. This can result in more than one distance computation, depending on the
    * already cached distances. This tries to make as few as possible.
    *
    * @param origins From which points to compute the distances.
    * @param destinations To which points to compute the distances.
    * @param travelMode The travel mode.
    * @param maybeTrafficHandling The traffic parameters, which are the departure time and the traffic estimation model.
    * @return An Async instance of a map containing results by segment (either an error or the resulting value).
    */
  final def batchDistances(
      origins: List[LatLong],
      destinations: List[LatLong],
      travelMode: TravelMode,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[Segment, Either[E, Distance]]] = {
    val maybeCachedValues =
      origins
        .flatMap(origin => destinations.map(Segment(origin, _)))
        .parTraverse { segment =>
          val maybeCachedValue =
            getCached(decoder, travelMode, segment.origin, segment.destination, maybeTrafficHandling)

          (segment.pure[F] -> maybeCachedValue).bisequence
        }

    maybeCachedValues.flatMap { cachedList =>
      val (uncachedValues, cachedValues) = cachedList.partition { case (_, maybeCached) => maybeCached.isEmpty }

      val cachedMap        = retrieveCachedValues(cachedValues)
      val distanceBatchesF = computeUnknownDistancesAndUpdateCache(uncachedValues, travelMode, maybeTrafficHandling)

      distanceBatchesF.map { distanceBatches =>
        distanceBatches.foldLeft(cachedMap) {
          case (accMap, batches) =>
            accMap ++ batches
        }
      }
    }
  }

  private def retrieveCachedValues(cachedValues: List[(Segment, Option[Distance])]): Map[Segment, Either[E, Distance]] =
    cachedValues
      .map { case (segment, cachedDistance) => segment -> Right[E, Distance](cachedDistance.get) }
      .toMap[Segment, Either[E, Distance]]

  private def computeUnknownDistancesAndUpdateCache(
      uncachedValues: List[(Segment, Option[Distance])],
      travelMode: TravelMode,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[List[Map[Segment, Either[E, Distance]]]] = {

    def cacheIfSuccessful(
        origin: LatLong,
        destination: LatLong,
        errorOrDistance: Either[E, Distance]
    ): F[(Segment, Either[E, Distance])] = {
      val eitherF: F[Either[E, Distance]] =
        maybeUpdateCache(errorOrDistance, travelMode, origin, destination, maybeTrafficHandling)

      (Segment(origin, destination).pure[F] -> eitherF).bisequence
    }

    val segments         = uncachedValues.map { case (segment, _) => segment }
    val segmentsByOrigin = segments.groupBy(_.origin)

    val groups =
      segmentsByOrigin
        .groupBy {
          case (_, segments) =>
            segments.map(_.destination).sortBy(destCoords => (destCoords.latitude, destCoords.longitude))
        }
        .mapValues(_.keys.toList)
        .toList

    groups.parTraverse {
      case (destinations, origins) =>
        batchDistanceF(travelMode, origins, destinations, maybeTrafficHandling)
          .flatMap { distancesMap =>
            distancesMap.toList
              .parTraverse {
                case (Segment(origin, destination), errorOrDistance) =>
                  cacheIfSuccessful(origin, destination, errorOrDistance)
              }
              .map(_.toMap)
          }
    }
  }

  final def distanceFromPostalCodes(geocoder: Geocoder[F])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[TravelMode, Either[E, Distance]]] =
    if (origin == destination) Async[F].pure(travelModes.map(_ -> Right(Distance.zero))(breakOut))
    else
      (
        geocoder.geocodePostalCode(origin),
        geocoder.geocodePostalCode(destination)
      ).parTupled.flatMap { case (orig, dest) => distance(orig, dest, travelModes, maybeTrafficHandling) }

  final def distances(paths: Seq[DirectedPathMultipleModes]): F[Map[DirectedPath, Either[E, Distance]]] = {
    val combinedDirectedPaths: List[DirectedPathMultipleModes] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPathMultipleModes(origin, destination, _, _) => (origin, destination) }(
          directedPathSemiGroup,
          breakOut
        )

    combinedDirectedPaths
      .parFlatTraverse {
        case DirectedPathMultipleModes(origin, destination, travelModes, maybeDepartureTime) =>
          if (origin == destination)
            travelModes
              .map { mode =>
                val zero: Either[E, Distance] = Right[E, Distance](Distance.zero)
                DirectedPath(origin, destination, mode, maybeDepartureTime) -> zero
              }
              .pure[F]
          else
            parDistances(travelModes, origin, destination, maybeDepartureTime)
      }
      .map(_.toMap)
  }

  private[this] final def parDistances(
      modes: List[TravelMode],
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[List[(DirectedPath, Either[E, Distance])]] =
    modes.parTraverse { mode =>
      distanceF(mode, origin, destination, maybeTrafficHandling).flatMap { errorOrDistance =>
        val eitherF: F[Either[E, Distance]] =
          maybeUpdateCache(errorOrDistance, mode, origin, destination, maybeTrafficHandling)

        (DirectedPath(origin, destination, mode, maybeTrafficHandling).pure[F] -> eitherF).bisequence
      }
    }

  private def maybeUpdateCache(
      errorOrDistance: Either[E, Distance],
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Either[E, Distance]] = {
    errorOrDistance match {
      case Right(distance) =>
        caching(distance, decoder, encoder, mode, origin, destination, maybeTrafficHandling).map(Right[E, Distance])

      case Left(error) => error.pure[F].map(Left[E, Distance])
    }
  }
}

object DistanceApi {
  val decoder: Decoder[Distance] = Distance.decoder
  val encoder: Encoder[Distance] = Distance.encoder

  final def apply[F[_]: Async: Parallel, E](
      distanceF: DistanceF[F, E],
      batchDistanceF: BatchDistanceF[F, E],
      caching: Caching[F, Distance],
      getCached: GetCached[F, Distance]
  ): DistanceApi[F, E] =
    new DistanceApi(distanceF, batchDistanceF, caching, getCached)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPathMultipleModes] =
    new Semigroup[DirectedPathMultipleModes] {
      override def combine(x: DirectedPathMultipleModes, y: DirectedPathMultipleModes): DirectedPathMultipleModes =
        DirectedPathMultipleModes(
          origin = x.origin,
          destination = x.destination,
          (x.travelModes ++ y.travelModes).distinct
        )
    }
}
