package com.colisweb.distances

import com.colisweb.distances.Configuration.{Google, Redis}
import com.colisweb.distances.caches.RedisConfiguration
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}

final case class Configuration(google: Google, redis: Redis)

object Configuration {

  final case class Google(apiKey: String)

  final case class Redis(host: String, port: Int) {
    def asConfiguration: RedisConfiguration = RedisConfiguration(host, port)
  }

  def load: Configuration = {
    implicitly[ConfigReader[Google]]
    implicitly[ConfigReader[Redis]]
    ConfigSource.default.loadOrThrow[Configuration]
  }
}
