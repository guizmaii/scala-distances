package com.colisweb.distances.re
import com.colisweb.distances.TravelMode
import com.colisweb.distances.Types.{DirectedPath, DirectedPathMultipleModes}

object Utils {

  def pathForEachTravelMode(path: DirectedPathMultipleModes): List[(TravelMode, DirectedPath)] =
    path.travelModes
      .map { travelMode =>
        travelMode -> DirectedPath(path.origin, path.destination, travelMode, path.maybeTrafficHandling)
      }
}
