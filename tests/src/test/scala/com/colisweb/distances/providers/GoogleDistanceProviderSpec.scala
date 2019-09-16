package com.colisweb.distances.providers

import java.time.Instant

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, IO}
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types._
import com.colisweb.distances.providers.google.{
  GoogleDistanceProvider,
  GoogleDistanceProviderError,
  GoogleGeoApiContext
}
import com.colisweb.distances.{DistanceProvider, TrafficModel}
import monix.eval.Task
import org.scalatest.{Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class GoogleDistanceProviderSpec extends WordSpec with Matchers {

  val loggingF: String => Unit = (s: String) => println(s.replaceAll("key=([^&]*)&", "key=REDACTED&"))

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"), loggingF)

  def passTests[F[+ _]: Concurrent: Parallel](runSync: F[Any] => Any): Unit = {
    val distanceApi: DistanceProvider[F, GoogleDistanceProviderError] = GoogleDistanceProvider[F](geoContext)

    val paris01 = LatLong(48.8640493, 2.3310526)
    val paris02 = LatLong(48.8675641, 2.34399)
    val paris18 = LatLong(48.891305, 2.3529867)

    s"says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      val distanceBetween01And02 = runSync(distanceApi.distance(Driving, paris01, paris02))
        .asInstanceOf[Right[GoogleDistanceProviderError, Distance]]

      val distanceBetween01And18 = runSync(distanceApi.distance(Driving, paris01, paris18))
        .asInstanceOf[Right[GoogleDistanceProviderError, Distance]]

      distanceBetween01And02.value.length should be < distanceBetween01And18.value.length
      distanceBetween01And02.value.duration should be < distanceBetween01And18.value.duration
    }

    "returns an error if asked for a past traffic" in {
      val origin          = LatLong(48.8640493, 2.3310526)
      val destination     = LatLong(48.8675641, 2.34399)
      val trafficHandling = TrafficHandling(Instant.now.minusSeconds(60), TrafficModel.BestGuess)
      val distanceApi     = GoogleDistanceProvider[F](geoContext)
      val tryResult       = runSync(distanceApi.distance(Driving, origin, destination, Some(trafficHandling)))

      tryResult shouldBe a[Left[_, _]]
    }
  }

  "GoogleDistanceProvider.distances" should {
    "pass tests with cats-effect IO" should {
      val globalExecutionContext: ExecutionContext = ExecutionContext.global
      implicit val ctx: ContextShift[IO]           = IO.contextShift(globalExecutionContext)

      passTests[IO](_.unsafeRunSync())
    }

    "pass tests with Monix Task" should {
      import monix.execution.Scheduler.Implicits.global

      passTests[Task](_.runSyncUnsafe(10 seconds))
    }
  }

}
