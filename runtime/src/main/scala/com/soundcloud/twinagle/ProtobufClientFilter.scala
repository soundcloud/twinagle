
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.io.Buf
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

private[twinagle] class ProtobufClientFilter[
    Req <: GeneratedMessage,
    Rep <: GeneratedMessage: GeneratedMessageCompanion
](
    path: String
) extends Filter[Req, Rep, Request, Response] {
  override def apply(
      request: Req,
      service: Service[Request, Response]
  ): Future[Rep] = {
    val httpRequest = serializeRequest(path, request)
    service(httpRequest).map(deserializeResponse)
  }

  private def serializeRequest(path: String, r: Req): Request = {
    val request = Request(Method.Post, path)
    request.contentType = "application/protobuf"
    request.content = Buf.ByteArray.Owned(r.toByteArray)
    request
  }

  private def deserializeResponse(response: Response): Rep = {
    val companion = implicitly[GeneratedMessageCompanion[Rep]]
    companion.parseFrom(Buf.ByteArray.Owned.extract(response.content))
  }
}
