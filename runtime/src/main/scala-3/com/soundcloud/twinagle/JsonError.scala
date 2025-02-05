package com.soundcloud.twinagle

import play.api.libs.json.{Json, OFormat}

/** JsonError is the JSON representation of `TwinagleException`s.
  *
  * If only there were some Language we could use to Define these kind of Interfaces
  * so that we could generate code to (de-)serialize the errors ;).
  */
private[twinagle] case class JsonError(
    code: String,
    msg: String,
    meta: Option[Map[String, String]]
)

private[twinagle] object JsonError {
  import scala.util.control.Exception.*

  implicit val _format: OFormat[JsonError] = Json.format[JsonError]

  def fromString(str: String): Option[JsonError] = allCatch opt {
    Json.parse(str).as[JsonError]
  }

  def toString(err: JsonError): String = Json.stringify(Json.toJson(err))
}
