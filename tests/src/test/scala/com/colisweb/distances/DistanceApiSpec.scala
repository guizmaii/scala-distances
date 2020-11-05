package com.colisweb.distances

import java.time.{Instant, ZonedDateTime}

import cats.MonadError
import cats.effect.{ContextShift, IO}
import com.colisweb.distances.DistanceApiSpec.RunSync
import com.colisweb.distances.caches.CaffeineCache
import com.colisweb.distances.model.path.DirectedPathWithModeAndSpeedAt
import com.colisweb.distances.model.{DistanceAndDuration, Point, TravelMode}
import com.colisweb.distances.providers.google.{GoogleDistanceApi, GoogleGeoApiContext, TrafficModel}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.caffeine.{CaffeineCache => CaffeineScalaCache}
import scalacache.{Flags, Mode}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class DistanceApiSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  private val caffeineInstance = CaffeineScalaCache.apply[Nothing]

  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  implicit val contextShift: ContextShift[IO]  = IO.contextShift(globalExecutionContext)

  val runSyncTry: RunSync[Try] = new RunSync[Try] {
    override def apply[A](fa: Try[A]): A = fa.get
  }

  val runAsyncIO: RunSync[IO] = new RunSync[IO] {
    override def apply[A](fa: IO[A]): A = fa.unsafeRunSync()
  }

  val runAsyncMonix: RunSync[Task] = new RunSync[Task] {
    implicit val monixScheduler: Scheduler = Scheduler.global
    override def apply[A](fa: Task[A]): A  = fa.runSyncUnsafe()
  }

  private val configuration                      = Configuration.load
  private val loggingF: String => Unit           = (s: String) => println(s.replaceAll("key=([^&]*)&", "key=REDACTED&"))
  private val googleContext: GoogleGeoApiContext = GoogleGeoApiContext(configuration.google.apiKey, loggingF)

  private val futureTime = ZonedDateTime.now().plusHours(1).toInstant
  private val pastTime   = ZonedDateTime.now().minusHours(1).toInstant

  private val paris01 = Point(48.8640493, 2.3310526)
  private val paris02 = Point(48.8675641, 2.34399)
  private val paris18 = Point(48.891305, 2.3529867)

  private val birdResults = Map(
    (paris01 -> paris02, DistanceAndDuration(1.0, 73L)),
    (paris01 -> paris18, DistanceAndDuration(3.4, 246L))
  )
  private val googleResults = Map(
    (paris01 -> paris02, DistanceAndDuration(1.4, 345L)),
    (paris01 -> paris18, DistanceAndDuration(4.5, 1064L))
  )

  def approximateTests[F[_]](
      api: DistanceApi[F, DirectedPathWithModeAndSpeedAt],
      results: Map[(Point, Point), DistanceAndDuration],
      trafficTime: Option[Instant],
      run: RunSync[F]
  ): Unit = {

    "return approximate distance and duration from Paris 01 to Paris 02 without traffic" in {
      val driveFrom01to02 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris02,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = trafficTime
      )
      val distanceFrom01to02 = run(api.distance(driveFrom01to02))

      distanceFrom01to02.distance shouldBe results(paris01 -> paris02).distance +- 0.1
      distanceFrom01to02.duration shouldBe results(paris01 -> paris02).duration +- 25L
    }

    "return approximate distance and duration from Paris 01 to Paris 18 without traffic" in {
      val driveFrom01to18 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = trafficTime
      )
      val distanceFrom01to18 = run(api.distance(driveFrom01to18))

      distanceFrom01to18.distance shouldBe results(paris01 -> paris18).distance +- 0.1
      distanceFrom01to18.duration shouldBe results(paris01 -> paris18).duration +- 25L
    }
  }

  def relativeTests[F[_]](
      api: DistanceApi[F, DirectedPathWithModeAndSpeedAt],
      trafficTime: Option[Instant],
      run: RunSync[F]
  ): Unit = {

    "return zero between the same points" in {
      val path = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris01,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = trafficTime
      )

      val distance = run(api.distance(path))

      distance shouldBe DistanceAndDuration.zero
    }

    "return smaller DistanceAndDuration from Paris 01 to Paris 02 than from Paris 01 to Paris 18" in {
      val driveFrom01to02 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris02,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = trafficTime
      )
      val driveFrom01to18 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = trafficTime
      )

      val distanceFrom01to02 = run(api.distance(driveFrom01to02))
      val distanceFrom01to18 = run(api.distance(driveFrom01to18))

      distanceFrom01to02.distance should be < distanceFrom01to18.distance
      distanceFrom01to02.duration should be < distanceFrom01to18.duration
    }

    // NB: Distance maybe longer, but Duration should be smaller
    "return smaller or equal Duration with traffic in Paris" in {
      val pathWithoutTraffic = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = None
      )
      val pathWithTraffic = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50.0,
        departureTime = Some(futureTime)
      )

      val distanceWithoutTraffic = run(api.distance(pathWithoutTraffic))
      val distanceWithTraffic    = run(api.distance(pathWithTraffic))

      distanceWithoutTraffic.duration should be <= distanceWithTraffic.duration
    }
  }

  def fTests[F[_]](
      run: RunSync[F],
      googleDistanceApi: GoogleDistanceApi[F, DirectedPathWithModeAndSpeedAt]
  )(implicit
      F: MonadError[F, Throwable],
      mode: Mode[F]
  ): Unit = {

    "Bird only" should {
      val distanceApi = Distances.haversine[F, DirectedPathWithModeAndSpeedAt].api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        None,
        run
      )
    }

    "Bird with Caffeine cache" should {
      val distanceApi = Distances
        .haversine[F, DirectedPathWithModeAndSpeedAt]
        .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = None,
        run
      )
    }

    "Google only" should {
      relativeTests(
        googleDistanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        googleDistanceApi,
        googleResults,
        trafficTime = None,
        run
      )
    }

    "Google with Caffeine cache" should {
      val distanceApi = Distances
        .from(googleDistanceApi)
        .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        googleResults,
        trafficTime = None,
        run
      )
    }

    "Google with fallback on Bird, and traffic in the past" should {
      val distanceApi = Distances
        .from(googleDistanceApi)
        .fallback(Distances.haversine[F, DirectedPathWithModeAndSpeedAt])
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = Some(pastTime),
        run
      )
    }
  }

  "DistanceApi" should {

    "sync with Try" should {
      import cats.implicits.catsStdInstancesForTry
      import scalacache.modes.try_._
      fTests(runSyncTry, GoogleDistanceApi.sync(googleContext, TrafficModel.BestGuess))
    }

    "async with IO" should {
      import scalacache.CatsEffect.modes.async
      fTests(
        runAsyncIO,
        GoogleDistanceApi.async[IO, DirectedPathWithModeAndSpeedAt](googleContext, TrafficModel.BestGuess)
      )
    }

    "async with Monix Task" should {
      import scalacache.CatsEffect.modes.async
      fTests(
        runAsyncMonix,
        GoogleDistanceApi.async[Task, DirectedPathWithModeAndSpeedAt](googleContext, TrafficModel.BestGuess)
      )
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    import scalacache.modes.try_._

    // clear caches
    caffeineInstance.doRemoveAll().get
    ()
  }
}

object DistanceApiSpec {

  trait RunSync[F[_]] {
    def apply[A](fa: F[A]): A
  }
}
