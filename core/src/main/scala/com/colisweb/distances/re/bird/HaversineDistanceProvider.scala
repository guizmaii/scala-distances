package com.colisweb.distances.re.bird
import cats.Applicative
import cats.data.Kleisli
import com.colisweb.distances.re.Distances
import com.colisweb.distances.re.model.Path.VelocityParameter
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import squants.space.Kilometers

import scala.concurrent.duration._

class HaversineDistanceProvider[F[_]: Applicative, R: VelocityParameter] extends Distances.Function[F, Nothing, R] {

  override def apply(path: Path[R]): F[Either[Nothing, DistanceAndDuration]] = {
    val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
    val velocity             = VelocityParameter[R].velocity(path.parameters)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, velocity)
    val distanceAndDuration  = DistanceAndDuration(Kilometers(distanceInKilometers), timeInSeconds.seconds)
    Applicative[F].pure(Right(distanceAndDuration))
  }
}

object HaversineDistanceProvider {

  def apply[F[_]: Applicative, R: VelocityParameter]: Distances.Function[F, Nothing, R] =
    new HaversineDistanceProvider

  def builder[F[_]: Applicative, R: VelocityParameter]: Distances.Builder[F, Nothing, R] = {
    val instance = apply
    Kleisli(instance.apply)
  }
}
