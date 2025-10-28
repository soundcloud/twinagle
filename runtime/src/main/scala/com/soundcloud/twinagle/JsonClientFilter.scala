
import com.twitter.finagle.http.{MediaType, Method, Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import scalapb.json4s.{JsonFormat, Parser}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

private[twinagle] class JsonClientFilter[
    Req <: GeneratedMessage,
    Rep <: GeneratedMessage: GeneratedMessageCompanion
](
    path: String
) extends Filter[Req, Rep, Request, Response] {

  private val jsonParser = new Parser().ignoringUnknownFields

  override def apply(
      request: Req,
      service: Service[Request, Response]
  ): Future[Rep] = {
    val httpRequest = serializeRequest(path, request)
    service(httpRequest).map(deserializeResponse)
  }

  def serializeRequest(path: String, r: Req): Request = {
    val request = Request(Method.Post, path)
    request.contentType = MediaType.Json
    request.contentString = JsonFormat.toJsonString(r)
    request
  }

  def deserializeResponse(response: Response): Rep = {
    jsonParser.fromJsonString[Rep](response.contentString)
  }
}
