package com.colisweb.distances.re.bird
import cats.Applicative
import com.colisweb.distances.re.DistanceApi
import com.colisweb.distances.re.model.Path.VelocityParameter
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}
import squants.space.Kilometers

import scala.concurrent.duration._

class BirdHaversineDistance[F[_]: Applicative, O: VelocityParameter] extends DistanceApi[F, Nothing, O] {

  override def distance(path: Path[O]): F[Either[Nothing, DistanceAndDuration]] = {
    val distanceInKilometers = Haversine.distance(path.origin, path.destination)
    val velocity             = VelocityParameter[O].velocity(path.parameters)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, velocity)
    val distanceAndDuration  = DistanceAndDuration(Kilometers(distanceInKilometers), timeInSeconds.seconds)
    Applicative[F].pure(Right(distanceAndDuration))
  }
}

object BirdHaversineDistance {

  def apply[F[_]: Applicative, O: VelocityParameter]: BirdHaversineDistance[F, O] =
    new BirdHaversineDistance
}
