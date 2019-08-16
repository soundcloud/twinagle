package com.soundcloud.twinagle

import com.soundcloud.twinagle.session.SessionCache
import com.soundcloud.twinagle.test.TestMessage
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Status}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class TwirpEndpointFilterSpec extends Specification {

  "content-type header" >> {
    trait Context extends Scope {
      val svc = new TwirpEndpointFilter[TestMessage, TestMessage] andThen
        Service.mk[TestMessage, TestMessage](msg => Future.value(msg))
    }

    "protobuf" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf"
        // empty body because TestMessage has no fields and serializes as empty content

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf; charset=UTF-8"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }
    }

    "json" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/json"
        request.contentString = "{}"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/json; charset=UTF-8"
        request.contentString = "{}"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }
    }

    "un-supported content-type" in new Context {
      val request = Request()
      request.contentType = "application/xml; charset=UTF-8"
      request.contentString = "{}"

      val Throw(ex: TwinagleException) = Await.result(svc(request).liftToTry)
      ex.code ==== ErrorCode.BadRoute
    }

    "unspecified" in new Context {
      val request = Request()
      request.contentString = "{}"

      val Throw(ex: TwinagleException) = Await.result(svc(request).liftToTry)
      ex.code ==== ErrorCode.BadRoute
    }

    "custom HTTP headers" >> {
      "are not populated when missing from SessionCache" in {
        val svc = new TwirpEndpointFilter[TestMessage, TestMessage] andThen
          Service.mk[TestMessage, TestMessage](msg => {
            SessionCache.context.get(SessionCache.customHeadersKey).map(_.keys.toSeq) match {
              case Some(Seq("Content-Type")) => Future.value(msg)
              case _ =>
                Future.exception(TwinagleException(ErrorCode.Internal, "custom headers should not be present"))
            }
          })
        val request = Request()
        request.contentType = "application/protobuf; charset=UTF-8"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }

      "are available to service when provided" in {
        val svc = new TwirpEndpointFilter[TestMessage, TestMessage] andThen
          Service.mk[TestMessage, TestMessage](msg => {
            SessionCache.context.get(SessionCache.customHeadersKey).flatMap(_.get("test")) match {
              case Some("value") => Future.value(msg)
              case _             => Future.exception(TwinagleException(ErrorCode.Internal, "can't find custom headers"))
            }
          })
        val request = Request()
        request.headerMap.put("test", "value")
        request.contentType = "application/protobuf; charset=UTF-8"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }
    }
  }

}
