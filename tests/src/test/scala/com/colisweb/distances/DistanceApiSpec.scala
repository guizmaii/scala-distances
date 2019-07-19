package com.colisweb.distances

import cats.effect.{Concurrent, ContextShift, IO}
import com.colisweb.distances.Cache.CachingF
import com.colisweb.distances.DistanceProvider.DistanceF
import com.colisweb.distances.Types.TravelMode.{Bicycling, Driving}
import com.colisweb.distances.Types._
import com.colisweb.distances.caches.{CaffeineCache, RedisCache, RedisConfiguration}
import com.colisweb.distances.providers.google.{GoogleGeoApiContext, GoogleGeoProvider}
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import cats.temp.par._
  import com.colisweb.distances.utils.Stubs._

  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  implicit val contextShift: ContextShift[IO]  = IO.contextShift(globalExecutionContext)

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"))

  "DistanceApi" should {
    "#distance" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cachingF: CachingF[IO, Distance] = CaffeineCache[IO](Some(1 days)).cachingF
          val distanceF: DistanceF[IO]         = distanceProviderStub[IO].distance
          val distanceApi: DistanceApi[IO]     = DistanceApi[IO](distanceF, cachingF)
          val latLong                          = LatLong(0.0, 0.0)
          val expectedResult                   = Map((Driving, Distance.zero), (Bicycling, Distance.zero))
          distanceApi.distance(latLong, latLong, Driving :: Bicycling :: Nil).unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distanceFromPostalCodes" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cachingF: CachingF[IO, Distance] = CaffeineCache[IO](Some(1 days)).cachingF
          val distanceF: DistanceF[IO]         = distanceProviderStub[IO].distance
          val distanceApi: DistanceApi[IO]     = DistanceApi[IO](distanceF, cachingF)
          val postalCode                       = PostalCode("59000")
          val expectedResult                   = Map((Driving, Distance.zero), (Bicycling, Distance.zero))
          distanceApi
            .distanceFromPostalCodes(geocoderStub)(postalCode, postalCode, Driving :: Bicycling :: Nil)
            .unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distances" should {
      "pass the same test suite than GoogleDistanceProvider" should {
        def passTests[F[+ _]: Concurrent: Par](runSync: F[Any] => Any, cachingF: CachingF[F, Distance]): Unit = {
          val geocoder: GeoProvider[F]    = GoogleGeoProvider[F](geoContext)
          val distanceApi: DistanceApi[F] = DistanceApi[F](mockedDistanceF, cachingF)

          "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
            val paris01 = runSync(geocoder.geocode(PostalCode("75001"))).asInstanceOf[LatLong]
            val paris02 = runSync(geocoder.geocode(PostalCode("75002"))).asInstanceOf[LatLong]
            val paris18 = runSync(geocoder.geocode(PostalCode("75018"))).asInstanceOf[LatLong]

            paris01 shouldBe LatLong(48.8640493, 2.3310526)
            paris02 shouldBe LatLong(48.8675641, 2.34399)
            paris18 shouldBe LatLong(48.891305, 2.3529867)

            val driveFrom01to02 = DirectedPath(origin = paris01, destination = paris02, Driving :: Nil)
            val driveFrom01to18 = DirectedPath(origin = paris01, destination = paris18, Driving :: Nil)

            val results = runSync(distanceApi.distances(Array(driveFrom01to02, driveFrom01to18)))
              .asInstanceOf[Map[(TravelMode, LatLong, LatLong), Distance]]

            // We only check the length as travel duration varies over time & traffic
            results.mapValues(_.length) shouldBe Map(
              (Driving, paris01, paris02) -> 1024.meters,
              (Driving, paris01, paris18) -> 3429.meters
            )

            results((Driving, paris01, paris02)).length should be < results((Driving, paris01, paris18)).length
            results((Driving, paris01, paris02)).duration should be < results((Driving, paris01, paris18)).duration
          }
        }

        "pass tests with cats-effect and Caffeine" should {
          passTests[IO](_.unsafeRunSync(), CaffeineCache[IO](Some(1 days)).cachingF)
        }

        "pass tests with cats-effect and Redis" should {
          passTests[IO](
            _.unsafeRunSync(),
            RedisCache[IO](RedisConfiguration("locahost", 6379), Some(1 days)).cachingF
          )
        }

        "pass tests with Monix and Caffeine" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](_.runSyncUnsafe(10 seconds), CaffeineCache[Task](Some(1 days)).cachingF)
        }

        "pass tests with Monix and Redis" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](
            _.runSyncUnsafe(10 seconds),
            RedisCache[Task](RedisConfiguration("locahost", 6379), Some(1 days)).cachingF
          )
        }
      }
    }

  }

}
