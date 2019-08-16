package com.soundcloud.twinagle

import com.soundcloud.twinagle.session.SessionCache
import com.twitter.finagle.Service
import com.twitter.finagle.http.{HeaderMap, Request, Response, Status}
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
      val Throw(ex: TwinagleException) =
        Await.result(client(request, svc).liftToTry)
      ex.code ==== ErrorCode.Internal
      ex.meta should haveKeys(
        "location",
        "http_error_code_from_intermediary",
        "status_code"
      )
    }

    Fragments.foreach(
      Seq(
        Status.BadRequest         -> ErrorCode.Internal,
        Status.Unauthorized       -> ErrorCode.Unauthenticated,
        Status.Forbidden          -> ErrorCode.PermissionDenied,
        Status.NotFound           -> ErrorCode.BadRoute,
        Status.TooManyRequests    -> ErrorCode.Unavailable,
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
          val Throw(ex: TwinagleException) =
            Await.result(client(request, svc).liftToTry)
          ex.code ==== errorCode
          ex.meta should haveKeys(
            "body",
            "http_error_code_from_intermediary",
            "status_code"
          )
        }
    }

  }

  "custom HTTP headers" >> {
    "populates headerMap from SessionCache" in {
      val response = Response(Status.Ok)
      val svc = Service.mk[Request, Response] { req =>
        req.headerMap.get("test") match {
          case Some("value") => Future.value(response)
          case _             => Future.exception(TwinagleException(ErrorCode.Internal, "custom headers not found"))
        }
      }
      SessionCache.context.let(SessionCache.customHeadersKey, HeaderMap("test" -> "value")) {
        Await.result(client(request, svc)) ==== response
      }
    }

    "leaves headerMap unchanged when nothing is in SessionCache" in {
      val response = Response(Status.Ok)
      val svc = Service.mk[Request, Response] { req =>
        req.headerMap.keys.toSeq match {
          case Nil => Future.value(response)
          case _   => Future.exception(TwinagleException(ErrorCode.Internal, "custom headers should not be present"))
        }
      }
      Await.result(client(request, svc)) ==== response
    }
  }
}
