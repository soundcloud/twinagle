
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
  import org.json4s.*
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}

  import scala.annotation.nowarn
  import scala.util.control.Exception.*
  implicit val formats: AnyRef & Formats = Serialization.formats(NoTypeHints)

  // known issue in json4s wrt Manifest usage that has been deprecated in scala3
  // will suppress this deprecation warning until this is fixed
  // https://github.com/json4s/json4s/issues/982
  @nowarn("cat=deprecation")
  def fromString(str: String): Option[JsonError] = allCatch opt {
    read[JsonError](str)
  }

  def toString(err: JsonError): String = write(err)
}
