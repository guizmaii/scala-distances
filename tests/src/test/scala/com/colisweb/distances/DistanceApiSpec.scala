package com.colisweb.distances

import java.time.Instant

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, IO}
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types._
import com.colisweb.distances.caches.{CaffeineCache, RedisCache, RedisConfiguration}
import com.colisweb.distances.providers.google.GoogleGeoApiContext
import monix.eval.Task
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import squants.space.LengthConversions._
import squants.space.Meters

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import com.colisweb.distances.utils.Stubs._

  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  implicit val contextShift: ContextShift[IO]  = IO.contextShift(globalExecutionContext)

  val loggingF: String => Unit = (s: String) => println(s.replaceAll("key=([^&]*)&", "key=REDACTED&"))

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"), loggingF)
  lazy val redisConfiguration: RedisConfiguration =
    RedisConfiguration(sys.env.getOrElse("REDIS_HOST", "127.0.0.1"), 6379)

  "DistanceApi" should {
    "#distance" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cache          = CaffeineCache[IO](Some(1 days))
          val stub           = distanceProviderStub[IO, Unit]
          val distanceApi    = DistanceApi[IO, Unit](stub.distance, stub.batchDistances, cache.caching, cache.get)
          val latLong        = LatLong(0.0, 0.0)
          val expectedResult = Map((Driving, Right(Distance.zero)), (Bicycling, Right(Distance.zero)))

          distanceApi.distance(latLong, latLong, Driving :: Bicycling :: Nil).unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distanceFromPostalCodes" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cache          = CaffeineCache[IO](Some(1 days))
          val stub           = distanceProviderStub[IO, Unit]
          val distanceApi    = DistanceApi[IO, Unit](stub.distance, stub.batchDistances, cache.caching, cache.get)
          val postalCode     = PostalCode("59000")
          val expectedResult = Map((Driving, Right(Distance.zero)), (Bicycling, Right(Distance.zero)))

          distanceApi
            .distanceFromPostalCodes(geocoderStub)(postalCode, postalCode, Driving :: Bicycling :: Nil)
            .unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distances" should {
      "pass the same test suite than GoogleDistanceProvider" should {
        def passTests[F[+ _]: Concurrent: Parallel](runSync: F[Any] => Any, cache: Cache[F]): Unit = {
          def removeFromCache(
              travelMode: TravelMode,
              origin: LatLong,
              destination: LatLong,
              maybeTrafficHandling: Option[TrafficHandling]
          ): Unit = {
            runSync(cache.remove(travelMode, origin, destination, maybeTrafficHandling))
            ()
          }

          def removeBatchFromCache(
              travelMode: TravelMode,
              origins: List[LatLong],
              destinations: List[LatLong],
              maybeTrafficHandling: Option[TrafficHandling]
          ): Unit =
            origins.flatMap(origin => destinations.map(origin -> _)).foreach {
              case (origin, destination) =>
                removeFromCache(travelMode, origin, destination, maybeTrafficHandling)
            }

          val distanceApi: DistanceApi[F, Unit] =
            DistanceApi[F, Unit](mockedDistanceF[F], mockedBatchDistanceF[F], cache.caching, cache.get)

          val errorDistanceApi: DistanceApi[F, Unit] =
            DistanceApi[F, Unit](mockedDistanceErrorF[F], mockedBatchDistanceErrorF[F], cache.caching, cache.get)

          val paris01 = LatLong(48.8640493, 2.3310526)
          val paris02 = LatLong(48.8675641, 2.34399)
          val paris18 = LatLong(48.891305, 2.3529867)

          "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
            val driveFrom01to02 = DirectedPathMultipleModes(origin = paris01, destination = paris02, Driving :: Nil)
            val driveFrom01to18 = DirectedPathMultipleModes(origin = paris01, destination = paris18, Driving :: Nil)

            val paths = Array(driveFrom01to02, driveFrom01to18)

            val results = runSync(distanceApi.distances(paths)).asInstanceOf[Map[DirectedPath, Either[Unit, Distance]]]

            // We only check the length as travel duration varies over time & traffic
            results.mapValues(_.right.get.length) shouldBe Map(
              DirectedPath(paris01, paris02, Driving, None) -> 1024.meters,
              DirectedPath(paris01, paris18, Driving, None) -> 3429.meters
            )

            results(DirectedPath(paris01, paris02, Driving, None)).right.get.length should be <
              results(DirectedPath(paris01, paris18, Driving, None)).right.get.length

            results(DirectedPath(paris01, paris02, Driving, None)).right.get.duration should be <
              results(DirectedPath(paris01, paris18, Driving, None)).right.get.duration

            paths.foreach(
              path => removeFromCache(path.travelModes.head, path.origin, path.destination, path.maybeTrafficHandling)
            )
          }

          val origin         = LatLong(48.8640493, 2.3310526)
          val destination    = LatLong(48.8675641, 2.34399)
          val length         = Meters(1024)
          val travelDuration = 73728.millis

          "not take into account traffic when not asked to with a driving travel mode" in {
            val expected = Map(Driving -> Right(Distance(length, travelDuration)))
            val result   = runSync(distanceApi.distance(origin, destination, Driving :: Nil, None))

            result shouldBe expected

            removeFromCache(Driving, origin, destination, None)
          }

          "take into account traffic when asked to with a driving travel mode and a best guess estimation" in {
            val expected        = Map(Driving -> Right(Distance(length, 5.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.BestGuess)

            val result = runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling)))

            result shouldBe expected
            removeFromCache(Driving, origin, destination, Some(trafficHandling))
          }

          "take into account traffic when asked to with a driving travel mode and an optimistic estimation" in {
            val expected        = Map(Driving -> Right(Distance(length, 2.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.Optimistic)

            val result = runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling)))

            result shouldBe expected
            removeFromCache(Driving, origin, destination, Some(trafficHandling))
          }

          "take into account traffic when asked to with a driving travel mode and a pessimistic estimation" in {
            val expected        = Map(Driving -> Right(Distance(length, 10.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.Pessimistic)

            val result = runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling)))

            result shouldBe expected
            removeFromCache(Driving, origin, destination, Some(trafficHandling))
          }

          "not take into account traffic when asked to with a bicycling travel mode" in {
            val expected        = Map(Bicycling -> Right(Distance(length, travelDuration)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.BestGuess)

            val result = runSync(distanceApi.distance(origin, destination, Bicycling :: Nil, Some(trafficHandling)))

            result shouldBe expected
            removeFromCache(Bicycling, origin, destination, Some(trafficHandling))
          }

          "say that Paris 02 is closer to Paris 01 than Paris 18 in a batch distances computation" in {
            val origins      = List(paris01)
            val destinations = List(paris02, paris18)

            val results =
              runSync(distanceApi.batchDistances(origins, destinations, Driving, None))
                .asInstanceOf[Map[Segment, Either[Unit, Distance]]]

            results(Segment(paris01, paris02)).right.get.length should be <
              results(Segment(paris01, paris18)).right.get.length

            results(Segment(paris01, paris02)).right.get.duration should be <
              results(Segment(paris01, paris18)).right.get.duration

            removeBatchFromCache(Driving, origins, destinations, None)
          }

          "not call for new computations when already cached with batch" in {
            val origins      = List(LatLong(48.86, 2.3))
            val destinations = List(LatLong(48.85, 2.3), LatLong(48.84, 2.3))

            val results =
              runSync(distanceApi.batchDistances(origins, destinations, Driving, None))
                .asInstanceOf[Map[Segment, Either[Unit, Distance]]]

            results.values.forall(_.isRight) shouldBe true

            val moreOrigins      = origins :+ LatLong(48.86, 2.4)
            val moreDestinations = destinations :+ LatLong(48.83, 2.3)

            val moreResults = runSync(errorDistanceApi.batchDistances(moreOrigins, moreDestinations, Driving, None))
              .asInstanceOf[Map[Segment, Either[Unit, Distance]]]

            val knownEntries =
              moreResults.filterKeys(path => origins.contains(path.origin) && destinations.contains(path.destination))

            val unknownEntries = moreResults.filterKeys(!knownEntries.contains(_))

            knownEntries.values.forall(_.isRight) shouldBe true
            unknownEntries.values.forall(_.isLeft) shouldBe true

            removeBatchFromCache(Driving, origins, destinations, None)
          }

          "not call for new computations when already cached with single calls" in {
            val origins      = List(LatLong(48.86, 2.3))
            val destinations = List(LatLong(48.85, 2.3), LatLong(48.84, 2.3))
            val paths        = origins.flatMap(origin => destinations.map(origin -> _))

            val results = paths
              .map { case (o, d) => distanceApi.distance(o, d, List(Driving), None) }
              .map(runSync(_).asInstanceOf[Map[TravelMode, Either[Unit, Distance]]])

            results.map(_.mapValues(ignoreDistanceValue)) should contain only Map(Driving -> right)
            results.forall(_.apply(Driving).isRight) shouldBe true

            val moreOrigins      = origins :+ LatLong(48.86, 2.4)
            val moreDestinations = destinations :+ LatLong(48.83, 2.3)
            val morePaths        = moreOrigins.flatMap(origin => moreDestinations.map(origin -> _))

            val moreResults = morePaths.map {
              case (o, d) =>
                Segment(o, d) -> runSync(errorDistanceApi.distance(o, d, List(Driving), None))
                  .asInstanceOf[Map[TravelMode, Either[Unit, Distance]]]
            }.toMap

            val knownEntries =
              moreResults.filterKeys(path => origins.contains(path.origin) && destinations.contains(path.destination))

            val unknownEntries = moreResults.filterKeys(!knownEntries.contains(_))

            knownEntries.values.map(_.mapValues(ignoreDistanceValue)) should contain only Map(Driving -> right)
            knownEntries.values.map(_.apply(Driving)).forall(_.isRight) shouldBe true
            unknownEntries.values.map(_.mapValues(ignoreDistanceValue)) should contain only Map(Driving -> left)
            unknownEntries.values.map(_.apply(Driving)).forall(_.isLeft) shouldBe true

            removeBatchFromCache(Driving, origins, destinations, None)
          }

          "return all the uncached distances in a batch distances computation" in {
            val origins      = List(LatLong(48.86, 2.33), LatLong(48.87, 2.34), LatLong(48.88, 2.35))
            val destinations = List(LatLong(48.85, 2.32), LatLong(48.84, 2.31), LatLong(48.83, 2.3))
            val segments     = origins.flatMap(origin => destinations.map(Segment(origin, _)))

            val results =
              runSync(distanceApi.batchDistances(origins, destinations, Driving, None))
                .asInstanceOf[Map[Segment, Either[Unit, Distance]]]

            val caches = segments.map { segment =>
              runSync(cache.get(Distance.decoder, Driving, segment.origin, segment.destination, None))
                .asInstanceOf[Option[Distance]]
            }

            results.keys should contain theSameElementsAs segments
            results.values.forall(_.isRight) shouldBe true
            caches.forall(_.isDefined) shouldBe true

            val moreOrigins      = List(LatLong(48.85, 2.33), LatLong(48.85, 2.34))
            val moreDestinations = List(LatLong(48.84, 2.36), LatLong(48.83, 2.37))
            val allOrigins       = origins ++ moreOrigins
            val allDestinations  = destinations ++ moreDestinations
            val allSegments      = allOrigins.flatMap(origin => allDestinations.map(Segment(origin, _)))

            val allResults =
              runSync(distanceApi.batchDistances(allOrigins, allDestinations, Driving, None))
                .asInstanceOf[Map[Segment, Either[Unit, Distance]]]

            val allCaches = allSegments.map { segment =>
              runSync(cache.get(Distance.decoder, Driving, segment.origin, segment.destination, None))
                .asInstanceOf[Option[Distance]]
            }

            allResults.keys should contain theSameElementsAs allSegments
            allResults.values.forall(_.isRight) shouldBe true
            allCaches.forall(_.isDefined) shouldBe true

            removeBatchFromCache(Driving, allOrigins, allDestinations, None)
          }
        }

        "pass tests with cats-effect and Caffeine" should {
          passTests[IO](_.unsafeRunTimed(10 seconds).get, CaffeineCache[IO](Some(1 days)))
        }

        "pass tests with cats-effect and Redis" should {
          passTests[IO](_.unsafeRunTimed(10 seconds).get, RedisCache[IO](redisConfiguration, Some(1 days)))
        }

        "pass tests with Monix and Caffeine" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](_.runSyncUnsafe(10 seconds), CaffeineCache[Task](Some(1 days)))
        }

        "pass tests with Monix and Redis" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](_.runSyncUnsafe(10 seconds), RedisCache[Task](redisConfiguration, Some(1 days)))
        }
      }
    }
  }

  private def ignoreDistanceValue(e: Either[Unit, Distance]): Either[Unit, Unit] = e.map(_ => ())
  private val right: Either[Unit, Unit]                                          = Right[Unit, Unit](())
  private val left: Either[Unit, Unit]                                           = Left[Unit, Unit](())
}
