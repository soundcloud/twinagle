package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

/** TwirpHttpClient
  */
private[twinagle] class TwirpHttpClient extends SimpleFilter[Request, Response] {
  override def apply(
      request: Request,
      service: Service[Request, Response]
  ): Future[Response] = {
    service(request).flatMap { response =>
      response.status match {
        case Status.Ok => Future.value(response)
        case _         => Future.exception(errorFromResponse(response))
      }
    }
  }

  // Error handling ported from the Go implementation of Twirp:
  // https://github.com/twitchtv/twirp/blob/3b7987b1a81780060352721385655fdcbf9c12da/protoc-gen-twirp/generator.go#L488-L528

  // errorFromResponse builds a TwinagleException from a non-200 HTTP response.
  // If the response has a valid serialized Twirp error, then it's returned.
  // If not, the response status code is used to generate a similar twirp
  // error. See twirpErrorFromIntermediary for more info on intermediary errors.
  private def errorFromResponse(httpResponse: Response): TwinagleException =
    if (isRedirect(httpResponse.status)) {
      // Unexpected redirect: it must be an error from an intermediary.
      // Twirp clients don't follow redirects automatically, Twirp only handles
      // POST requests, redirects should only happen on GET and HEAD requests.
      val location = httpResponse.location.getOrElse("")
      val status   = httpResponse.status
      val msg      = s"unexpected HTTP status $status received, Location=$location"
      errorFromIntermediary(status, msg, location)
    } else {
      JsonError.fromString(httpResponse.contentString) match {
        case Some(body) /* to love */ =>
          ErrorCode.fromString(body.code) match {
            case Some(errorCode) =>
              TwinagleException(
                errorCode,
                body.msg,
                body.meta.getOrElse(Map.empty)
              )
            case None =>
              TwinagleException(
                ErrorCode.Internal,
                s"invalid type returned from server error response: ${body.code}"
              )
          }
        // Invalid JSON response; it must be an error from an intermediary.
        case None =>
          errorFromIntermediary(
            httpResponse.status,
            s"Error from intermediary with HTTP status ${httpResponse.status}",
            httpResponse.contentString
          )
      }
    }

  // errorFromIntermediary maps HTTP errors from non-twirp sources to twirp errors.
  // The mapping is similar to gRPC: https://github.com/grpc/grpc/blob/master/doc/http-grpc-status-mapping.md.
  // Returned twirp Errors have some additional metadata for inspection.
  private def errorFromIntermediary(
      status: Status,
      msg: String,
      bodyOrLocation: String
  ): TwinagleException = {
    import Status._
    val errorCode = status match {
      case s if isRedirect(s)                               => ErrorCode.Internal
      case BadRequest                                       => ErrorCode.Internal
      case Unauthorized                                     => ErrorCode.Unauthenticated
      case Forbidden                                        => ErrorCode.PermissionDenied
      case NotFound                                         => ErrorCode.BadRoute
      case TooManyRequests                                  => ErrorCode.ResourceExhausted
      case BadGateway | ServiceUnavailable | GatewayTimeout =>
        ErrorCode.Unavailable
      case _ => ErrorCode.Internal
    }

    val bodyOrLocationKey = if (isRedirect(status)) "location" else "body"
    val meta              = Map(
      "http_error_code_from_intermediary" -> "true",
      "status_code"                       -> status.code.toString,
      bodyOrLocationKey                   -> bodyOrLocation
    )
    TwinagleException(errorCode, msg, meta)
  }

  private def isRedirect(status: Status): Boolean =
    status.code >= 300 && status.code <= 399
}
