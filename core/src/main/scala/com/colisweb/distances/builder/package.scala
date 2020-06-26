package com.colisweb.distances

import cats.{Monad, Parallel}

package object builder {

  def builders[F[_]](implicit F: Monad[F]): Builders[F] = new Builders[F] {
    override protected implicit val M: Monad[F] = F
  }

  def parallel[F[_]](implicit F: Monad[F], PF: Parallel[F]): ParallelBuilders[F] = new ParallelBuilders[F] {
    override protected implicit val M: Monad[F]    = F
    override protected implicit val P: Parallel[F] = PF
  }
}
