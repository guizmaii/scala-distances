package com.colisweb.distances.cache

trait CacheKey {
  def parts: Seq[Any]
}

trait ProductCacheKey extends Product with CacheKey {
  override def parts: Seq[Any] = productIterator.toSeq
}
