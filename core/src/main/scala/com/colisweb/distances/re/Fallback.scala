package com.colisweb.distances.re
import cats.Monad
import cats.implicits._
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

class Fallback[F[_]: Monad, E, O](
    private val first: DistanceApi[F, _, O],
    private val second: DistanceApi[F, E, O]
) extends DistanceApi[F, E, O] {

  override def distance(path: Path[O]): F[Either[E, DistanceAndDuration]] =
    first.distance(path).flatMap {
      case Right(value) => Monad[F].pure(Right(value))
      case Left(_)      => second.distance(path)
    }
}

class FallbackOptional[F[_]: Monad, E, O](
    private val first: DistanceOptionApi[F, O],
    private val second: DistanceApi[F, E, O]
) extends DistanceApi[F, E, O] {

  override def distance(path: Path[O]): F[Either[E, DistanceAndDuration]] =
    first.distance(path).flatMap {
      case Some(value) => Monad[F].pure(Right(value))
      case None        => second.distance(path)
    }
}

object Fallback {

  def apply[F[_]: Monad, E, O](first: DistanceOptionApi[F, O], second: DistanceApi[F, E, O]): DistanceApi[F, E, O] =
    new FallbackOptional(first, second)

  def apply[F[_]: Monad, E, O](first: DistanceApi[F, _, O], second: DistanceApi[F, E, O]): DistanceApi[F, E, O] =
    new Fallback(first, second)
}
