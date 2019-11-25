package com.colisweb.distances.caches

import cats.effect.{Async, IO}
import com.colisweb.distances.Cache
import com.colisweb.distances.Types._
import io.circe._
import io.circe.generic.semiauto._
import monix.eval.Task
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import squants.space.Length

import scala.concurrent.duration._
import scala.language.postfixOps

final case class Toto(
    name: String,
    age: Int,
    latLong: LatLong,
    distance: Distance
)

object Toto {
  implicit final val decoder: Decoder[Toto] = deriveDecoder[Toto]
  implicit final val Encoder: Encoder[Toto] = deriveEncoder[Toto]
}

class CacheSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  import com.colisweb.distances.generators.Gens._
  import com.colisweb.distances.utils.circe.LengthSerializer._
  import com.colisweb.distances.utils.circe.ScalaDurationSerializer._
  import io.circe.literal._

  implicitly[Decoder[Duration]] // IntelliJ doesn't understand the need of `import ScalaDerivation._` without this
  implicitly[Decoder[Length]]   // IntelliJ doesn't understand the need of `import LengthSerializer._` without this

  // TODO Jules: Implement test for JSON serialization
  def expectedJson(toto: Toto): Json =
    json"""
      {
        "name" : ${toto.name},
        "age" : ${toto.age},
        "latLong" : {
          "latitude" : ${toto.latLong.latitude},
          "longitude" : ${toto.latLong.longitude}
        },
        "distance" : {
          "length" : ${toto.distance.length},
          "duration" : ${toto.distance.duration}
        }
      }
    """

  def tests[F[+ _]](cacheImpl: () => Cache[F])(runSync: F[Any] => Any)(implicit F: Async[F]): Unit = {
    val cache = cacheImpl()

    "cache" should {
      "save things, access them and remove them" in {
        forAll(travelModeGen, latLongGen, latLongGen, distanceGen) { (mode, origin, destination, distance) =>
          // Cache with cachingF
          runSync(
            cache.cachingF(F.pure(distance), Distance.decoder, Distance.encoder, mode, origin, destination)
          ).asInstanceOf[Distance] shouldBe distance

          // Cache with cachingF and an error
          runSync(
            cache.cachingF(
              F.raiseError(new RuntimeException).asInstanceOf[F[Distance]],
              Distance.decoder,
              Distance.encoder,
              mode,
              origin,
              destination
            )
          ).asInstanceOf[Distance] shouldBe distance

          // Retrieve value
          runSync(
            cache.get(Distance.decoder, mode, origin, destination)
          ).asInstanceOf[Option[Distance]] shouldBe Some(distance)

          // Cache with caching
          runSync(
            cache.caching(distance, Distance.decoder, Distance.encoder, origin, destination, mode)
          ).asInstanceOf[Distance] shouldBe distance

          // Retrieve value
          runSync(
            cache.get(Distance.decoder, origin, destination, mode)
          ).asInstanceOf[Option[Distance]] shouldBe Some(distance)

          // Remove values
          runSync(cache.remove(mode, origin, destination))
          runSync(cache.remove(origin, destination, mode))

          // Verify they are removed
          runSync(cache.get(Distance.decoder, mode, origin, destination)).asInstanceOf[Option[Distance]] shouldBe None
          runSync(cache.get(Distance.decoder, origin, destination, mode)).asInstanceOf[Option[Distance]] shouldBe None
        }
      }
    }
  }

  def noCacheTests[F[+ _]](runSync: F[Any] => Any)(implicit F: Async[F]): Unit = {
    val noCache = NoCache[F]()

    "no cache" should {
      "not save things and not access them" in {
        forAll(travelModeGen, latLongGen, latLongGen, distanceGen) { (mode, origin, destination, distance) =>
          // Cache with cachingF
          runSync(
            noCache.cachingF(F.pure(distance), Distance.decoder, Distance.encoder, mode, origin, destination)
          ).asInstanceOf[Distance] shouldBe distance

          // Retrieve value
          runSync(
            noCache.get(Distance.decoder, mode, origin, destination)
          ).asInstanceOf[Option[Distance]] shouldBe None

          // Cache with caching
          runSync(
            noCache.caching(distance, Distance.decoder, Distance.encoder, origin, destination, mode)
          ).asInstanceOf[Distance] shouldBe distance

          // Retrieve value
          runSync(
            noCache.get(Distance.decoder, origin, destination, mode)
          ).asInstanceOf[Option[Distance]] shouldBe None
        }
      }
    }
  }

  "with cats-effect IO" should {
    "with CaffeineCache" should {
      tests[IO](() => CaffeineCache(Some(1 day)))(_.unsafeRunSync())
    }

    "pass RedisCache" should {
      tests[IO](() => RedisCache(redisConfiguration, Some(1 day)))(_.unsafeRunSync())
    }

    "pass NoCache" should {
      noCacheTests[IO](_.unsafeRunSync())
    }
  }

  "with Monix Task" should {
    import monix.execution.Scheduler.Implicits.global

    "with CaffeineCache" should {
      tests[Task](() => CaffeineCache(Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }

    "pass RedisCache" should {
      tests[Task](() => RedisCache(redisConfiguration, Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }

    "pass NoCache" should {
      noCacheTests[Task](_.runSyncUnsafe(10 seconds))
    }
  }

  def redisConfiguration =
    RedisConfiguration(sys.env.getOrElse("REDIS_HOST", "127.0.0.1"), 6379)
}
