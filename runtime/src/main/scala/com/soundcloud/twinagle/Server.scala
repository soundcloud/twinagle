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
  * - extensibility/observability (metrics, logging, tracing, exception tracking, etc)
  * - do (de-)serialization errors need special handling?
  * - check for POST method
  *
  * @param endpoints list of endpoints to expose
  */
class Server(endpoints: Seq[Endpoint]) extends Service[Request, Response] {

  private val services = endpoints.foldLeft(Map.empty[String, Service[Request, Response]]) { (acc, ep) =>
    acc + (ep.path -> ep.service)
  }

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
