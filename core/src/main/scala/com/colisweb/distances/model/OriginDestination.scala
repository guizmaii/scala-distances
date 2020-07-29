package com.colisweb.distances.model

trait OriginDestination[P] {
  def origin(path: P): Point
  def destination(path: P): Point
}

trait OriginDestinationData {
  def origin: Point
  def destination: Point
}

object OriginDestination {

  implicit def forData[P <: OriginDestinationData]: OriginDestination[P] = new OriginDestination[P] {
    override def origin(path: P): Point      = path.origin
    override def destination(path: P): Point = path.destination
  }
}
