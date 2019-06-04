package com.soundcloud.twinagle

import com.soundcloud.twinagle.test.TestMessage
import com.twitter.finagle.http.{Request, Status}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ServiceAdapterSpec extends Specification {

  "content-type header" >> {
    trait Context extends Scope {
      val serviceAdapter = new ServiceAdapter[TestMessage, TestMessage](m => Future.value(m))
    }

    "protobuf" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf"
        // empty body because TestMessage has no fields and serializes as empty content

        val response = Await.result(serviceAdapter(request))

        response.status ==== Status.Ok
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf; charset=UTF-8"

        val response = Await.result(serviceAdapter(request))

        response.status ==== Status.Ok
      }
    }

    "json" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/json"
        request.contentString = "{}"

        val response = Await.result(serviceAdapter(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/json; charset=UTF-8"
        request.contentString = "{}"

        val response = Await.result(serviceAdapter(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }
    }

    "un-supported content-type" in new Context {
      val request = Request()
      request.contentType = "application/xml; charset=UTF-8"
      request.contentString = "{}"

      val Throw(ex: TwinagleException) = Await.result(serviceAdapter(request).liftToTry)
      ex.code ==== ErrorCode.BadRoute
    }

    "unspecified" in new Context {
      val request = Request()
      request.contentString = "{}"

      val Throw(ex: TwinagleException) = Await.result(serviceAdapter(request).liftToTry)
      ex.code ==== ErrorCode.BadRoute
    }
  }

}
