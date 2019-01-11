package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future


/**
  * A Twirp-compatible HTTP server/router. Takes a path-to-service mapping for all RPC methods.
  * TwinagleExceptions are returned as conformant JSON responses, other exceptions are mapped
  * to twirp internal errors.
  *
  * TODO:
  * - should the path live with the service somehow?
  * - extensibility/observability (metrics, logging, tracing, exception tracking, etc)
  * - do (de-)serialization errors need special handling?
  * - check for POST method
  *
  * @param services
  */
class Server(val services: Map[String, Service[Request, Response]]) extends Service[Request, Response] {

  override def apply(request: Request): Future[Response] = services.get(request.path) match {
    case Some(service) => service(request).handle {
      case e: TwinagleException => errorResponse(e)
      case e => errorResponse(new TwinagleException(e))
    }
    case None => Future.value(
      errorResponse(TwinagleException(
        ErrorCode.BadRoute,
        s"unknown path: ${request.path}"
      ))
    )

  }

  private def errorResponse(twex: TwinagleException): Response = {
    val resp = Response(twex.code.status)
    resp.contentType = MediaType.Json
    // todo: proper JSON
    resp.contentString =
      s"""
         |{
         |  "code": "${twex.code.desc}",
         |  "msg": "${twex.msg}",
         |  "meta": {}
         |}
         """.stripMargin
    resp
  }
}
