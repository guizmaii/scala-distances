package com.colisweb.distances.providers

import cats.effect.{Concurrent, ContextShift, IO}
import cats.temp.par.Par
import com.colisweb.distances.Types.TravelMode.Driving
import com.colisweb.distances.Types._
import com.colisweb.distances.providers.google.{GoogleDistanceProvider, GoogleGeoApiContext, GoogleGeoProvider}
import com.colisweb.distances.{DistanceProvider, GeoProvider}
import monix.eval.Task
import org.scalatest.{Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class GoogleDistanceProviderSpec extends WordSpec with Matchers {

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"))

  def passTests[F[+ _]: Concurrent: Par](runSync: F[Any] => Any): Unit = {
    val geocoder: GeoProvider[F]         = GoogleGeoProvider[F](geoContext)
    val distanceApi: DistanceProvider[F] = GoogleDistanceProvider[F](geoContext)

    s"says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      val paris01 = runSync(geocoder.geocode(PostalCode("75001"))).asInstanceOf[LatLong]
      val paris02 = runSync(geocoder.geocode(PostalCode("75002"))).asInstanceOf[LatLong]
      val paris18 = runSync(geocoder.geocode(PostalCode("75018"))).asInstanceOf[LatLong]

      paris01 shouldBe LatLong(48.8640493, 2.3310526)
      paris02 shouldBe LatLong(48.8675641, 2.34399)
      paris18 shouldBe LatLong(48.891305, 2.3529867)

      val distanceBetween01And02 = runSync(distanceApi.distance(Driving, paris01, paris02)).asInstanceOf[Distance]
      val distanceBetween01And18 = runSync(distanceApi.distance(Driving, paris01, paris18)).asInstanceOf[Distance]

      // We only check the length as travel duration varies over time & traffic
      distanceBetween01And02.length shouldBe 2136.0.meters
      distanceBetween01And18.length shouldBe 4747.0.meters

      distanceBetween01And02.length should be < distanceBetween01And18.length
      distanceBetween01And02.duration should be < distanceBetween01And18.duration
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
