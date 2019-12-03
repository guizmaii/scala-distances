package com.colisweb.distances.providers.google.re

import com.colisweb.distances.TrafficModel.{BestGuess, Optimistic, Pessimistic}
import com.colisweb.distances.TravelMode.{Bicycling, Driving, Transit, Unknown, Walking}
import com.colisweb.distances.re.model.Point
import com.colisweb.distances.{TrafficModel, TravelMode}
import com.google.maps.model.TrafficModel.{BEST_GUESS, OPTIMISTIC, PESSIMISTIC}
import com.google.maps.model.TravelMode._
import com.google.maps.model.{
  LatLng => GoogleLatLong,
  TrafficModel => GoogleTrafficModel,
  TravelMode => GoogleTravelMode
}

object GoogleModel {

  implicit final class GoogleTrafficModelOps(model: TrafficModel) {
    def asGoogle: GoogleTrafficModel = model match {
      case BestGuess   => BEST_GUESS
      case Optimistic  => OPTIMISTIC
      case Pessimistic => PESSIMISTIC
    }
  }

  implicit final class GoogleTravelModeOps(mode: TravelMode) {
    def asGoogle: GoogleTravelMode = mode match {
      case Driving   => DRIVING
      case Bicycling => BICYCLING
      case Walking   => WALKING
      case Transit   => TRANSIT
      case Unknown   => UNKNOWN
    }
  }

  implicit final class GoogleLatLongOps(point: Point) {
    def asGoogle: GoogleLatLong = new GoogleLatLong(point.latitude, point.longitude)
  }
}
