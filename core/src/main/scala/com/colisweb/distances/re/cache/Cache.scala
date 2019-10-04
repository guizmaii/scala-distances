package com.colisweb.distances.re.cache

trait Cache[F[_], K, V] extends CacheGet[F, K, V] with CacheSet[F, K, V]

trait CacheGet[F[_], K, V] {

  def get(key: K): F[Option[V]]
}

trait CacheSet[F[_], K, V] {

  def set(key: K, value: V): F[Unit]
}

trait HashKey[T] {
  def hashKey(t: T): String
}
object HashKey {
  def apply[T](implicit HK: HashKey[T]): HashKey[T] = HK
}
