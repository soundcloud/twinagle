package com.soundcloud.twinagle

/**
  * JsonError is the JSON representation of `TwinagleException`s.
  *
  * If only there were some Language to Define these kind of Interfaces
  * tha we could use to generate code to (de-)serialize them ;).
  */
private[twinagle] case class JsonError(
    code: String,
    msg: String,
    meta: Option[Map[String, String]]
)

private[twinagle] object JsonError {
  // we use json4s because we depend on it already via scalapb-runtime.

  import org.json4s._
  import org.json4s.jackson.Serialization
  import org.json4s.jackson.Serialization.{read, write}

  import scala.util.control.Exception._
  implicit val formats = Serialization.formats(NoTypeHints)

  def fromString(str: String): Option[JsonError] = allCatch opt {
    read[JsonError](str)
  }

  def toString(err: JsonError): String = write(err)
}
