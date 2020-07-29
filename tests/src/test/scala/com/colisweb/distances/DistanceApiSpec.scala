package com.colisweb.distances

import java.time.ZonedDateTime

import cats.Applicative
import cats.effect.{ContextShift, IO}
import com.colisweb.distances.DistanceApiSpec.RunSync
import com.colisweb.distances.caches.CaffeineCache
import com.colisweb.distances.model.{DirectedPathWithModeAndSpeedAt, DistanceAndDuration, Point, TravelMode}
import com.colisweb.distances.providers.google.{GoogleDistanceApi, GoogleGeoApiContext, TrafficModel}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import scalacache.caffeine.{CaffeineCache => CaffeineScalaCache}
import scalacache.{Flags, Mode}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
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

  private val currentTime = ZonedDateTime.now().plusHours(1).toInstant
  private val paris01     = Point(48.8640493, 2.3310526)
  private val paris02     = Point(48.8675641, 2.34399)
  private val paris18     = Point(48.891305, 2.3529867)

  def apiTests[F[_]](api: DistanceApi[F, DirectedPathWithModeAndSpeedAt], run: RunSync[F]): Unit = {

    "return zero between the same points" in {
      val path = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris01,
        travelMode = TravelMode.Driving,
        speed = 50,
        departureTime = Some(currentTime)
      )

      val distance = run(api.distance(path))

      distance shouldBe DistanceAndDuration.zero
    }

    "return smaller DistanceAndDuration from Paris 01 to Paris 02 than from Paris 01 to Paris 18" in {
      val driveFrom01to02 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris02,
        travelMode = TravelMode.Driving,
        speed = 50,
        departureTime = Some(currentTime)
      )
      val driveFrom01to18 = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50,
        departureTime = Some(currentTime)
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
        speed = 50,
        departureTime = None
      )
      val pathWithTraffic = DirectedPathWithModeAndSpeedAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Driving,
        speed = 50,
        departureTime = Some(currentTime)
      )

      val distanceWithoutTraffic = run(api.distance(pathWithoutTraffic))
      val distanceWithTraffic    = run(api.distance(pathWithTraffic))

      distanceWithoutTraffic.duration should be <= distanceWithTraffic.duration
    }
  }

  def fTests[F[_]: Applicative](
      run: RunSync[F],
      googleDistanceApi: GoogleDistanceApi[F, DirectedPathWithModeAndSpeedAt]
  )(
      implicit mode: Mode[F]
  ): Unit = {

    "Bird only" should {
      apiTests(
        Distances.haversine[F, DirectedPathWithModeAndSpeedAt].api,
        run
      )
    }

    "Bird with Caffeine cache" should {
      apiTests(
        Distances
          .haversine[F, DirectedPathWithModeAndSpeedAt]
          .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
          .api,
        run
      )
    }

    "Google only" should {
      apiTests(
        googleDistanceApi,
        run
      )
    }

    "Google with Caffeine cache" should {
      apiTests(
        Distances
          .from(googleDistanceApi)
          .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
          .api,
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
