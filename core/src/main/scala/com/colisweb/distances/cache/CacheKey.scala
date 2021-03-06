package com.colisweb.distances.cache

trait CacheKey[-K] {
  def parts(key: K): Seq[Any]
}

object CacheKey {

  class ProductCacheKey[-K <: Product] extends CacheKey[K] {
    override def parts(key: K): Seq[Any] = key.productIterator.toList
  }

  implicit val forProduct: CacheKey[Product] = new ProductCacheKey[Product]

  implicit class CacheKeySyntax[-K](key: K)(implicit cacheKey: CacheKey[K]) {
    def parts: Seq[Any] = cacheKey.parts(key)
  }
}
