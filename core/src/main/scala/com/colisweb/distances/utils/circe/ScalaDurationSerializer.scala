package com.colisweb.distances.utils.circe

import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.Duration

object ScalaDurationSerializer {

  implicit final val durationEncoder: Encoder[Duration] =
    Encoder.instance(duration => Json.fromString(duration.toString))

  /**
    * (strongly) Inspired by circe-java8 `java.time.Duration` Decoder.
    *
    * See: https://github.com/circe/circe/blob/master/modules/java8/src/main/scala/io/circe/java8/time/TimeInstances.scala#L184
    */
  implicit final val durationDecoder: Decoder[Duration] = Decoder.instance { c =>
    c.as[String] match {
      case Right(s) =>
        try Right(Duration(s))
        catch {
          case _: NumberFormatException => Left(DecodingFailure("Scala Duration", c.history))
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[Duration]]
    }
  }

}
