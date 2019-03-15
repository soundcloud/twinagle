package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http._
import com.twitter.util.Future


/**
  * A Twirp-compatible HTTP server/router.
  * TwinagleExceptions are returned as conformant JSON responses, other exceptions are mapped
  * to twirp internal errors.
  *
  * TODO:
  * - extensibility/observability (metrics, logging, tracing, exception tracking, etc)
  * - do (de-)serialization errors need special handling?
  *
  * @param endpoints list of endpoints to expose
  */
class Server(endpoints: Map[EndpointMetadata, Service[Request, Response]]) extends Service[Request, Response] {

  private val servicesByPath = endpoints.map { case (k, v) => k.path -> v }

  override def apply(request: Request): Future[Response] = request.method match {
    case Method.Post =>
      servicesByPath.get(request.path) match {
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
    case method =>
      Future.value(
        errorResponse(TwinagleException(
          ErrorCode.BadRoute,
          s"unsupported method $method (only POST is allowed)"
        ))
      )
  }

  val endpointMetadataByPath: Map[String, EndpointMetadata] = endpoints.map(e => (e._1.path, e._1))

  private def errorResponse(twex: TwinagleException): Response = {
    import ErrorCode._
    val resp = Response(twex.code match {
      case Canceled |
           DeadlineExceeded => Status.RequestTimeout
      case NotFound |
           BadRoute => Status.NotFound
      case PermissionDenied |
           ResourceExhausted => Status.Forbidden
      case Unauthenticated => Status.Unauthorized
      case FailedPrecondition => Status.PreconditionFailed
      case AlreadyExists |
           Aborted => Status.Conflict
      case InvalidArgument |
           OutOfRange => Status.BadRequest
      case Unimplemented => Status.NotImplemented
      case Unknown |
           Internal |
           Dataloss => Status.InternalServerError
      case Unavailable => Status.ServiceUnavailable
    })
    resp.contentType = MediaType.Json
    resp.contentString = JsonError.toString(JsonError(
      code = twex.code.desc,
      msg = twex.msg,
      meta = if (twex.meta.nonEmpty) Some(twex.meta) else None
    ))
    resp
  }
}
