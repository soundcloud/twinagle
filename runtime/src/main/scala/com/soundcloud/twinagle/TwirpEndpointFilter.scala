
import com.twitter.finagle.http.{MediaType, Request, Response, Status}
import com.twitter.finagle.{Filter, Service}
import com.twitter.io.Buf
import com.twitter.util.Future
import scalapb.json4s.{Parser, Printer}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

/** Decodes Protobuf or Json-encoded HTTP requests into Twirp messages
  *
  * @tparam Req incoming twirp request message
  * @tparam Rep outgoing twirp response message
  */
private[twinagle] class TwirpEndpointFilter[
    Req <: GeneratedMessage: GeneratedMessageCompanion,
    Rep <: GeneratedMessage: GeneratedMessageCompanion
] extends Filter[Request, Response, Req, Rep] {

  private val jsonParser = new Parser().ignoringUnknownFields
  private val printer    = new Printer().includingDefaultValueFields

  override def apply(
      request: Request,
      service: Service[Req, Rep]
  ): Future[Response] = request.mediaType match {
    case Some(MediaType.Json) =>
      val input = jsonParser.fromJsonString[Req](request.contentString)
      service(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = MediaType.Json
        response.contentString = printer.print(r)
        response
      }
    case Some("application/protobuf") =>
      val input = implicitly[GeneratedMessageCompanion[Req]]
        .parseFrom(toBytes(request.content))
      service(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = "application/protobuf"
        response.content = Buf.ByteArray.Owned(r.toByteArray)
        response
      }
    case _ =>
      val contentType = request.contentType.getOrElse("")
      Future.exception(
        TwinagleException(
          ErrorCode.BadRoute,
          s"unexpected Content-Type: '$contentType'"
        )
      )
  }

  private def toBytes(buf: Buf): Array[Byte] = Buf.ByteArray.Owned.extract(buf)
}
