package com.colisweb.distances

import java.time.Instant

import cats.effect.{ContextShift, IO}
import com.colisweb.distances.DistanceApiSpec.GoogleProviderMock
import com.colisweb.distances.bird.{DurationFromSpeed, Haversine}
import com.colisweb.distances.caches.{CaffeineCache, RedisConfiguration}
import com.colisweb.distances.model.{DistanceAndDuration, PathWithModeAndSpeedAt, Point, TravelMode}
import com.colisweb.distances.providers.google.{GoogleDistanceApi, GoogleDistanceProvider, GoogleGeoApiContext}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import scalacache.Flags

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  import scalacache.CatsEffect.modes.async

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
          val cache    = CaffeineCache[IO, PathWithModeAndSpeedAt, DistanceAndDuration](Flags.defaultFlags, Some(1 days))
          val provider = new GoogleProviderMock()

          val distanceApi =
            DistanceBuilder.withCacheKey(new GoogleDistanceApi[IO, PathWithModeAndSpeedAt](provider)).cache(cache).build
          val point          = Point(0.0, 0.0)
          val expectedResult = DistanceAndDuration(0.0, 0)
          val path           = PathWithModeAndSpeedAt(point, point, TravelMode.Driving, 10, None)
          distanceApi.distance(path).unsafeRunSync() shouldBe expectedResult
        }
      }
    }
  }
}

object DistanceApiSpec {
  class GoogleProviderMock extends GoogleDistanceProvider[IO](null, null, null) {
    override def singleRequest(
        travelMode: TravelMode,
        origin: Point,
        destination: Point,
        departureTime: Option[Instant]
    ): IO[DistanceAndDuration] = {
      val distance = Haversine.distanceInKm(origin, destination)
      val duration = DurationFromSpeed.durationForDistance(distance, 50)
      IO.pure(DistanceAndDuration(distance, duration))
    }
  }
}
