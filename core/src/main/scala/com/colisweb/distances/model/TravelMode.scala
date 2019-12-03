package com.colisweb.distances.model

import cats.Show
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TravelMode extends EnumEntry
object TravelMode extends Enum[TravelMode] {
  val values: immutable.IndexedSeq[TravelMode] = findValues

  case object Driving   extends TravelMode
  case object Bicycling extends TravelMode
  case object Walking   extends TravelMode
  case object Transit   extends TravelMode
  case object Unknown   extends TravelMode

  implicit final val show: Show[TravelMode] = new Show[TravelMode] {
    override def show(travelMode: TravelMode): String =
      travelMode match {
        case Driving   => "driving"
        case Bicycling => "bicycling"
        case Walking   => "walking"
        case Transit   => "transit"
        case Unknown   => "unknown"
      }
  }
}
