package com.colisweb.distances.re
import cats.Monad
import com.colisweb.distances.re.model.{DistanceAndDuration, Path}

import cats.implicits._

class FallbackBatch[F[_]: Monad, E, O](
    private val first: DistanceBatchApi[F, _, O],
    private val second: DistanceBatchApi[F, E, O]
) extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    first.distances(paths).flatMap { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[Path[O], Either[E, DistanceAndDuration]], List.empty[Path[O]])
      ) {
        case ((s, e), (path, Left(_)))      => (s, e :+ path)
        case ((s, e), (path, Right(value))) => (s.updated(path, Right(value)), e)
      }
      if (missing.nonEmpty) {
        second.distances(missing).map(values ++ _)
      } else {
        Monad[F].pure(values)
      }
    }
}

class FallbackBatchOptional[F[_]: Monad, E, O](
    private val first: DistanceBatchOptionApi[F, O],
    private val second: DistanceBatchApi[F, E, O]
) extends DistanceBatchApi[F, E, O] {

  override def distances(paths: List[Path[O]]): F[Map[Path[O], Either[E, DistanceAndDuration]]] =
    first.distances(paths).flatMap { results =>
      val (values, missing) = results.foldLeft(
        (Map.empty[Path[O], Either[E, DistanceAndDuration]], List.empty[Path[O]])
      ) {
        case ((s, e), (path, None))        => (s, e :+ path)
        case ((s, e), (path, Some(value))) => (s.updated(path, Right(value)), e)
      }
      if (missing.nonEmpty) {
        second.distances(missing).map(values ++ _)
      } else {
        Monad[F].pure(values)
      }
    }
}

object FallbackBatch {

  def apply[F[_]: Monad, E, O](
      first: DistanceBatchApi[F, _, O],
      second: DistanceBatchApi[F, E, O]
  ): DistanceBatchApi[F, E, O] = new FallbackBatch[F, E, O](first, second)

  def apply[F[_]: Monad, E, O](
      first: DistanceBatchOptionApi[F, O],
      second: DistanceBatchApi[F, E, O]
  ): DistanceBatchApi[F, E, O] = new FallbackBatchOptional[F, E, O](first, second)
}
