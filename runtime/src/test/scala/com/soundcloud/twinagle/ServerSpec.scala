package com.soundcloud.twinagle

import com.soundcloud.twinagle.test.TestMessage
import com.twitter.finagle.http.{MediaType, Method, Request, Status}
import com.twitter.util.{Await, Future}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import com.twitter.finagle.CancelledRequestException

class ServerSpec extends Specification with Mockito {
  trait Context extends Scope {
    val rpc = mock[TestMessage => Future[TestMessage]]
    val protoService = ProtoService(
      Seq(
        ProtoRpc(EndpointMetadata("svc", "rpc"), rpc)
      )
    )
    val server = ServerBuilder()
      .register(protoService)
      .build
  }

  private def httpRequest(method: Method = Method.Post, path: String = "/twirp/svc/rpc") = {
    val req = Request(method, path)
    req.mediaType = MediaType.Json
    req.contentString = "{}"
    req
  }

  "happy case" in new Context {
    val request = httpRequest()
    rpc.apply(any) returns Future.value(TestMessage())

    val response = Await.result(server(request))

    there was exactly(1)(rpc).apply(TestMessage())
    response.status ==== Status.Ok
  }

  "non-POST request" in new Context {
    val request = httpRequest(method = Method.Get)

    val response = Await.result(server(request))

    there were noCallsTo(rpc)
    response.status ==== Status.NotFound // TODO: really? verify w/ Go impl
  }

  "unknown path" in new Context {
    val request = httpRequest(path = "/bar")

    val response = Await.result(server(request))

    there were noCallsTo(rpc)
    response.status ==== Status.NotFound
  }

  "exceptions" >> {
    "TwinagleException" in new Context {
      val request = httpRequest()
      val ex      = TwinagleException(ErrorCode.PermissionDenied, "nope")
      rpc.apply(any) returns Future.exception(ex)

      val response = Await.result(server(request))

      there was exactly(1)(rpc).apply(TestMessage())
      response.status ==== Status.Forbidden
      response.contentString must contain(ex.code.desc)
    }

    "unknown exceptions" in new Context {
      val request = httpRequest()
      val ex      = new RuntimeException("eek")
      rpc.apply(any) returns Future.exception(ex)

      val response = Await.result(server(request))

      there was exactly(1)(rpc).apply(TestMessage())
      response.status ==== Status.InternalServerError
      response.contentString must contain(ErrorCode.Internal.desc)
      response.contentString must contain(ex.toString)
    }

    "catches exceptions thrown by user code" in new Context {
      val request = httpRequest()
      val ex      = new RuntimeException("eek")
      rpc.apply(any) throws ex

      val response = Await.result(server(request))

      there was exactly(1)(rpc).apply(TestMessage())
      response.status ==== Status.InternalServerError
      response.contentString must contain(ErrorCode.Internal.desc)
      response.contentString must contain(ex.toString)
    }

    "translates Finagle cancelled request exceptions into Twirp canceled" in new Context {
      val request = httpRequest()
      val ex      = new CancelledRequestException()
      rpc.apply(any) returns Future.exception(ex)

      val response = Await.result(server(request))

      there was exactly(1)(rpc).apply(TestMessage())
      response.status ==== Status.RequestTimeout
      response.contentString must contain(ErrorCode.Canceled.desc)
      response.contentString must contain("Request canceled by client")
    }
  }
}
