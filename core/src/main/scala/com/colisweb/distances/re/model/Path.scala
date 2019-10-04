package com.colisweb.distances.re.model
import java.time.Instant

import com.colisweb.distances.TravelMode
import squants.motion.Velocity

case class Path[R](origin: Point, destination: Point, parameters: R)

object Path {

  type PathSimple                    = Path[Unit]
  type PathVelocity                  = Path[Velocity]
  type PathTravelMode                = Path[TravelMode]
  type PathTravelModeTraffic         = Path[TravelModeTraffic]
  type PathTravelModeTrafficVelocity = Path[TravelModeTrafficVelocity]

  def apply(origin: Point, destination: Point): Path[Unit] = Path(origin, destination, ())

  case class TravelModeTraffic(travelMode: TravelMode, traffic: Option[Instant])
  case class TravelModeTrafficVelocity(travelMode: TravelMode, traffic: Option[Instant], velocity: Velocity)

  trait VelocityParameter[R] {
    def velocity(parameters: R): Velocity
  }
  object VelocityParameter {
    def apply[R](implicit V: VelocityParameter[R]): VelocityParameter[R] = V
    def extract[R](f: R => Velocity): VelocityParameter[R]               = (parameters: R) => f(parameters)
    implicit val self: VelocityParameter[Velocity]                       = identity[Velocity]
  }

  trait TravelModeParameter[R] {
    def travelMode(parameters: R): TravelMode
  }
  object TravelModeParameter {
    def apply[R](implicit T: TravelModeParameter[R]): TravelModeParameter[R] = T
    def extract[R](f: R => TravelMode): TravelModeParameter[R]               = (parameters: R) => f(parameters)
    implicit val self: TravelModeParameter[TravelMode]                       = identity[TravelMode]
  }

  trait DepartureTimeParameter[R] {
    def departureTime(parameters: R): Instant
  }
  object DepartureTimeParameter {
    def apply[R](implicit T: DepartureTimeParameter[R]): DepartureTimeParameter[R] = T
    def extract[R](f: R => Instant): DepartureTimeParameter[R]                     = (parameters: R) => f(parameters)
    implicit val self: DepartureTimeParameter[Instant]                             = identity[Instant]
  }
}
