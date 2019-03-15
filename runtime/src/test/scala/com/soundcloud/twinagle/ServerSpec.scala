package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ServerSpec extends Specification with Mockito {

  trait Context extends Scope {
    val svc = mock[Service[Request, Response]]
    val server = new Server(Map(
      EndpointMetadata("/twirp", "svc", "rpc") -> svc
    ))
  }

  "happy case" in new Context {
    val request = Request(Method.Post, "/twirp/svc/rpc")
    svc.apply(any) returns Future.value(Response(Status.Ok))


    val response = Await.result(server(request))

    there was exactly(1)(svc).apply(request)
    response.status ==== Status.Ok
  }

  "non-POST request" in new Context {
    val request = Request(Method.Get, "/twirp/svc/rpc")

    val response = Await.result(server(request))

    there were noCallsTo(svc)
    response.status ==== Status.NotFound // TODO: really? verify w/ Go impl
  }

  "unknown path" in new Context {
    val request = Request(Method.Post, "/bar")

    val response = Await.result(server(request))

    there were noCallsTo(svc)
    response.status ==== Status.NotFound
  }

  "handles TwinagleException" in new Context {
    val request = Request(Method.Post, "/twirp/svc/rpc")
    val ex = TwinagleException(ErrorCode.PermissionDenied, "nope")
    svc.apply(any) returns Future.exception(ex)


    val response = Await.result(server(request))

    there was exactly(1)(svc).apply(request)
    response.status ==== Status.Forbidden
    response.contentString must contain(ex.code.desc)
  }

  "handles unknown exceptions" in new Context {
    val request = Request(Method.Post, "/twirp/svc/rpc")
    val ex = new RuntimeException("eek")
    svc.apply(any) returns Future.exception(ex)


    val response = Await.result(server(request))

    there was exactly(1)(svc).apply(request)
    response.status ==== Status.InternalServerError
    response.contentString must contain(ErrorCode.Internal.desc)
    response.contentString must contain(ex.toString)
  }

  "doesn't catch exceptions" in new Context {
    // TBD: do we want this?
    val request = Request(Method.Post, "/twirp/svc/rpc")
    val ex = new RuntimeException("eek")
    svc.apply(any) throws ex

    server(request) must throwAn(ex)
  }

}
