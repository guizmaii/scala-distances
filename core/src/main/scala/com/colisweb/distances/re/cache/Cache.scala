package com.colisweb.distances.re.cache

trait Cache[F[_], K, V] {

  def get(key: K): F[Option[V]]
  def put(key: K, value: V): F[Any]
}
