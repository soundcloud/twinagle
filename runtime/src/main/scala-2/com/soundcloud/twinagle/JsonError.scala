
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
  // we use json4s because we depend on it already via scalapb-runtime.

  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}

  import scala.util.control.Exception._
  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)

  def fromString(str: String): Option[JsonError] = allCatch opt {
    read[JsonError](str)
  }

  def toString(err: JsonError): String = write(err)
}
