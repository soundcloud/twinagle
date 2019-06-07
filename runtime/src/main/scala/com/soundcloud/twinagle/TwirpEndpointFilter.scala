package com.soundcloud.twinagle

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{MediaType, Request, Response, Status}
import com.twitter.io.Buf
import com.twitter.util.Future
import scalapb.json4s.JsonFormat
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

/**
  * Decodes Protobuf or Json-encoded HTTP requests into Twirp messages
  *
  * @tparam Req incoming twirp request message
  * @tparam Rep outgoing twirp response message
  */
private[twinagle] class TwirpEndpointFilter[
    Req <: GeneratedMessage with Message[Req]: GeneratedMessageCompanion,
    Rep <: GeneratedMessage with Message[
      Rep
    ]: GeneratedMessageCompanion
] extends Filter[Request, Response, Req, Rep] {

  override def apply(
      request: Request,
      service: Service[Req, Rep]
  ): Future[Response] = request.mediaType match {
    case Some(MediaType.Json) =>
      val input = JsonFormat.fromJsonString[Req](request.contentString)
      service(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = MediaType.Json
        response.contentString = JsonFormat.toJsonString(r)
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
