package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response, Status}
import com.twitter.io.Buf
import com.twitter.util.Future
import scalapb.json4s.JsonFormat
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

// runtime
class JsonConverter {
  def mkRequest(path: String, r: GeneratedMessage): Request = {
    val request = Request(path)
    request.contentType = MediaType.Json
    request.contentString = JsonFormat.toJsonString(r)
    request
  }

  def mkResponse[Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](response: Response): Rep = {
    JsonFormat.fromJsonString[Rep](response.contentString)
  }
}


/**
  * Assumes that each PB service produces a generated trait
  *
  * ```
  * trait SomeService {
  * def foo(in: FooReq): Future[FooResp]
  * def bar(in: BarReq): Future[BarResp]
  * }
  * ```
  *
  * Allows turning service methods into `Service[http.Request, http.Reponse]`:
  *
  * ```
  * new Server(Map(
  * "/twirp/com.soundcloud.SomeService/Foo" -> new ServiceAdapter(myFooService.foo _),
  * "/twirp/com.soundcloud.SomeService/Bar" -> new ServiceAdapter(myBarService.bar _),
  * ))
  * ```
  *
  * Supports JSON and binary Protobuf, depending on the request Content-Type header.
  *
  * @param f
  * @param companion
  * @tparam Req
  * @tparam Rep
  */
class ServiceAdapter[Req <: GeneratedMessage with Message[Req] : GeneratedMessageCompanion, Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](f: Req => Future[Rep])

  extends Service[Request, Response] {

  override def apply(request: Request): Future[Response] = request.contentType match {
    case Some(MediaType.Json) =>
      val input = JsonFormat.fromJsonString[Req](request.contentString)
      f(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = MediaType.Json
        response.contentString = JsonFormat.toJsonString(r)
        response
      }
    case Some("application/protobuf") =>
      val input = implicitly[GeneratedMessageCompanion[Req]].parseFrom(toBytes(request.content))
      f(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = "application/protobuf"
        response.content = Buf.ByteArray.Owned(r.toByteArray)
        response
      }
    case Some(other) => Future.exception(TwinagleException(
      ErrorCode.Internal, // TODO: correct error code
      s"unknown Content-Type: $other"
    ))
    case None => Future.exception(TwinagleException(
      ErrorCode.Internal, // TODO: correct error code
      s"no Content-Type header supplied"
    ))
  }

  private def toBytes(buf: Buf): Array[Byte] = Buf.ByteArray.Owned.extract(buf)

}
