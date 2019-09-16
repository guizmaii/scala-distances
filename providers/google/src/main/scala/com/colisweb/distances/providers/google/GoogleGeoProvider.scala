package com.colisweb.distances.providers.google

import cats.Parallel
import cats.effect.{Concurrent, Sync}
import com.google.maps.model.{ComponentFilter, LatLng => GoogleLatLng}
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.colisweb.distances.GeoProvider
import com.colisweb.distances.Types.{LatLong, NonAmbiguousAddress, Point, PostalCode}

import scala.util.control.NoStackTrace

sealed abstract class GoogleGeoProviderError(message: String) extends RuntimeException(message) with NoStackTrace
final case class PostalCodeNotFound(message: String)          extends GoogleGeoProviderError(message)
final case class NonAmbiguousAddressNotFound(message: String) extends GoogleGeoProviderError(message)

/**
  * Remarques:
  * ---------
  *
  * Les résultats de Geocoding de code postaux sont très hasardeux si l'on ne précise que le code postal.
  * Par exemple, si l'on utilise "59000", la réponse sera la bonne tandis que si l'on utilise "59130" la réponse
  * sera vide donc fausse.
  *
  * Il est possible de préciser d'autres "components" comme le "country" cependant nous n'avons pas forcément le pays.
  *
  * Un astuce qui a été trouvé est de préciser la "region" comme étant "eu" (Europe). Cela semble améliorer les
  * résultat. Par exemple, avec "region=eu", "59130" obtient une réponse valide.
  *
  * Seconde astuce, il faut également préciser le "language" sinon un pourcentage élevé de réponses sont fausses.
  */
object GoogleGeoProvider {

  import cats.implicits._
  import com.colisweb.distances.providers.google.utils.Implicits._

  final def apply[F[_]: Concurrent: Parallel](geoApiContext: GoogleGeoApiContext): GeoProvider[F] =
    new GeoProvider[F] {

      private final def rawRequest: F[GeocodingApiRequest] =
        Sync[F].delay {
          GeocodingApi
            .newRequest(geoApiContext.geoApiContext)
            .region("eu")
            .language("fr")
        }

      override final def geocode(point: Point): F[LatLong] = point match {

        case postalCode: PostalCode =>
          rawRequest
            .flatMap { request =>
              request
                .components(ComponentFilter.postalCode(postalCode.value))
                .asEffect[F]
                .flatMap { response =>
                  Sync[F]
                    .fromOption(
                      response.headOption.map(_.geometry.location).map(asLatLong),
                      PostalCodeNotFound(s"${postalCode.show} is not found by Google API")
                    )
                }
            }

        /*
         * Doc about "non ambigue addresses": https://developers.google.com/maps/documentation/geocoding/best-practices#complete-address
         *
         *
         * WARNING:
         * --------
         *
         * The actual implementation tries to be robust: it will try to always give you an answer:
         *
         *   1. If it doesn't find an answer with the address information, it will try to geocode the address without the `line2` information.
         *   2. If it still doesn't find an answer, it will try to geocode the address without the `line2` and wihout the `town` information.
         *   3. Last, if it still doesn't find an answer, it will geocode the postal code.
         *
         * This situation is not ideal because, in the situation 2., the geocoder can potentially gives you a wrong answer.
         *
         * As the name of this method indicates, this geocoder method should normally be strict: it finds the exact location or it doesn't find any.
         *
         * For a more clever geocoder, Google proposes a better solution that what we're doing here.
         * See: https://developers.google.com/maps/documentation/geocoding/best-practices#automated-system
         *
         * TODO: The next step for this lib is to implement this more clever solution, but for now, use this method preferably with good quality data and/or with caution.
         */
        case address: NonAmbiguousAddress =>
          def fetch(addr: NonAmbiguousAddress): F[LatLong] =
            rawRequest
              .flatMap { request =>
                request
                  .components(ComponentFilter.country(addr.country))
                  .components(ComponentFilter.postalCode(addr.postalCode))
                  .address(s"${addr.line1} ${addr.line2} ${addr.town}")
                  .asEffect[F]
                  .flatMap { response =>
                    Sync[F]
                      .fromOption(
                        response.headOption.map(_.geometry.location).map(asLatLong),
                        NonAmbiguousAddressNotFound(s""""${addr.show}" is not found by Google API""")
                      )
                  }
              }

          fetch(address)
            .handleErrorWith { initialError =>
              (
                fetch(address.copy(line2 = "")),
                fetch(address.copy(line2 = "", town = "")),
                geocode(PostalCode(address.postalCode))
              ).raceInOrder3
                .handleErrorWith { _ =>
                  Sync[F].raiseError(initialError) // get back the original error
                }
            }
      }
    }

  @inline
  private[this] final def asLatLong(googleLatLng: GoogleLatLng): LatLong =
    LatLong(latitude = googleLatLng.lat, longitude = googleLatLng.lng)

}
