package com.colisweb.distances.util

object TestTypes {

  sealed trait Error
  case object Error extends Error

  sealed trait FirstError
  case object FirstError extends FirstError

  sealed trait SecondError
  case object SecondError extends SecondError

  case class IdParam(id: String)
}
