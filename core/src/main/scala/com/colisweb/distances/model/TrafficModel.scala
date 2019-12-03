package com.colisweb.distances.model

import cats.Show
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TrafficModel extends EnumEntry
object TrafficModel extends Enum[TravelMode] {
  val values: immutable.IndexedSeq[TravelMode] = findValues

  case object BestGuess   extends TrafficModel
  case object Optimistic  extends TrafficModel
  case object Pessimistic extends TrafficModel

  implicit final val show: Show[TrafficModel] = new Show[TrafficModel] {
    override def show(trafficModel: TrafficModel): String =
      trafficModel match {
        case BestGuess   => "best_guess"
        case Optimistic  => "optimistic"
        case Pessimistic => "pessimistic"
      }
  }
}
