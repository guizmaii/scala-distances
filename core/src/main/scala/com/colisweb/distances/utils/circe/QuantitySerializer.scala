package com.colisweb.distances.utils.circe

import io.circe._
import squants.space.Length
import squants.{Dimension, Quantity, UnitOfMeasure}

/**
  * Inspired by here:
  *   https://github.com/typelevel/squants/blob/master/shared/src/test/scala/squants/experimental/json/QuantitySerializer.scala
  *
  * @tparam A
  */
abstract class QuantitySerializer[A <: Quantity[A]] {

  import cats.implicits._
  import io.circe.syntax._

  protected val dimension: Dimension[A]

  private final lazy val symbolToUnit: String => Option[UnitOfMeasure[A]] =
    dimension.units.map(u => u.symbol -> u).toMap.get

  implicit final lazy val quantityEncoder: Encoder[A] =
    Encoder.instance(a => Json.obj("value" := a.value, "unit" := a.unit.symbol))

  implicit final lazy val quantityDecoder: Decoder[A] = Decoder.instance { c =>
    (c.downField("value").as[Double], c.downField("unit").as[String]).tupled.flatMap {
      case (value, unit) =>
        symbolToUnit(unit) match {
          case Some(u) => Right(u(value))
          case None    => Left(DecodingFailure(s"Could not find matching unit for symbol $unit", c.history))
        }
    }
  }

}
object LengthSerializer extends QuantitySerializer[Length] {
  override protected final val dimension: Dimension[Length] = Length
}
