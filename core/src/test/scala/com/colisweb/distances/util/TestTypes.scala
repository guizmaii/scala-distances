package com.colisweb.distances.util

import com.colisweb.distances.model.DistanceError

object TestTypes {

  sealed trait Error extends DistanceError
  case object Error  extends Error

  sealed trait FirstError extends DistanceError
  case object FirstError  extends FirstError

  sealed trait SecondError extends DistanceError
  case object SecondError  extends SecondError

  case class IdParam(id: String)
}
