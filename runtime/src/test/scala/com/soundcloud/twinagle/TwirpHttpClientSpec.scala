package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

class TwirpHttpClientSpec extends Specification {
  val client           = new TwirpHttpClient
  val request: Request = Request("/")

  "happy case (200)" in {
    val response = Response(Status.Ok)
    val svc      = Service.const(Future.value(response))
    Await.result(client(request, svc)) ==== response
  }

  "error handling" >> {
    "redirect" in {
      val response = Response(Status.MovedPermanently)
      val svc      = Service.const(Future.value(response))
      Await.result(client(request, svc).liftToTry) match {
        case Throw(ex: TwinagleException) =>
          ex.code ==== ErrorCode.Internal
          ex.meta should haveKeys(
            "location",
            "http_error_code_from_intermediary",
            "status_code"
          )
        case _ => ko
      }
    }

    Fragments.foreach(
      Seq(
        Status.BadRequest         -> ErrorCode.Internal,
        Status.Unauthorized       -> ErrorCode.Unauthenticated,
        Status.Forbidden          -> ErrorCode.PermissionDenied,
        Status.NotFound           -> ErrorCode.BadRoute,
        Status.TooManyRequests    -> ErrorCode.ResourceExhausted,
        Status.BadGateway         -> ErrorCode.Unavailable,
        Status.ServiceUnavailable -> ErrorCode.Unavailable,
        Status.GatewayTimeout     -> ErrorCode.Unavailable,
        // unexpected codes
        Status.EnhanceYourCalm     -> ErrorCode.Internal,
        Status.InternalServerError -> ErrorCode.Internal
      )
    ) {
      case (status, errorCode) =>
        s"HTTP ${status.code} produces $errorCode" ! {
          val response = Response(status)
          val svc      = Service.const(Future.value(response))
          Await.result(client(request, svc).liftToTry) match {
            case Throw(ex: TwinagleException) =>
              ex.code ==== errorCode
              ex.meta should haveKeys(
                "body",
                "http_error_code_from_intermediary",
                "status_code"
              )
            case _ => ko
          }
        }
    }
  }
}
