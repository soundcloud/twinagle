package com.soundcloud.twinagle

import com.soundcloud.twinagle.test.TestMessage
import com.twitter.finagle.http.*
import com.twitter.finagle.{CancelledRequestException, Failure, Filter, Service}
import com.twitter.util.{Await, Future}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import org.scalamock.specs2.MockContext

import scala.collection.mutable.ListBuffer

class ServerSpec extends Specification {

  trait Context extends Scope with MockContext {
    val rpc: TestMessage => Future[TestMessage] = mock[TestMessage => Future[TestMessage]]

    val protoService: ProtoService = ProtoService(
      Seq(
        ProtoRpcBuilder(EndpointMetadata("svc", "rpc"), rpc)
      )
    )
    val server: Service[Request, Response] = ServerBuilder()
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

    rpc.apply.expects(TestMessage()).returning(Future.value(TestMessage())).once()

    val response = Await.result(server(request))

    response.status ==== Status.Ok
  }

  "non-POST request" in new Context {
    val request = httpRequest(method = Method.Get)

    rpc.apply.expects(*).never()

    val response = Await.result(server(request))

    response.status ==== Status.NotFound // TODO: really? verify w/ Go impl
  }

  "unknown path" in new Context {
    val request = httpRequest(path = "/bar")

    rpc.apply.expects(*).never()

    val response = Await.result(server(request))

    response.status ==== Status.NotFound
  }

  "custom path prefix" in new Context {
    override val server: Service[Request, Response] = ServerBuilder()
      .withPrefix("/foo")
      .register(protoService)
      .build
    val request = httpRequest(path = "/foo/svc/rpc")
    rpc.apply.expects(TestMessage()).returning(Future.value(TestMessage())).once()

    val response = Await.result(server(request))

    response.status ==== Status.Ok

  }

  "generated message filter" in new Context {
    val recorderRequests = ListBuffer[GeneratedMessage]()
    val filter = new MessageFilter {
      override def toFilter[
          Req <: GeneratedMessage: GeneratedMessageCompanion,
          Resp <: GeneratedMessage: GeneratedMessageCompanion
      ]: Filter[Req, Resp, Req, Resp] =
        (request: Req, next: Service[Req, Resp]) => {
          recorderRequests += request
          next(request)
        }
    }
    override val server: Service[Request, Response] = ServerBuilder()
      .withPrefix("/foo")
      .withMessageFilter(filter)
      .register(protoService)
      .build
    val request = httpRequest(path = "/foo/svc/rpc")
    val message = TestMessage()
    rpc.apply.expects(*).returning(Future.value(message)).once()

    val response = Await.result(server(request))
    response.status ==== Status.Ok

    recorderRequests.toList ==== List(message)
  }

  "exceptions" >> {
    "TwinagleException" in new Context {
      val request = httpRequest()
      val ex      = TwinagleException(ErrorCode.PermissionDenied, "nope")
      rpc.apply.expects(TestMessage()).returning(Future.exception(ex)).once()

      val response = Await.result(server(request))

      response.status ==== Status.Forbidden
      response.contentString must contain(ex.code.desc)
    }

    "unknown exceptions" in new Context {
      val request = httpRequest()
      val ex      = new RuntimeException("eek")
      rpc.apply.expects(TestMessage()).returning(Future.exception(ex)).once()

      val response = Await.result(server(request))

      response.status ==== Status.InternalServerError
      response.contentString must contain(ErrorCode.Internal.desc)
      response.contentString must contain(ex.toString)
    }

    "catches exceptions thrown by user code" in new Context {
      val request = httpRequest()
      val ex      = new RuntimeException("eek")
      rpc.apply.expects(TestMessage()).once().throws(ex)

      val response = Await.result(server(request))

      response.status ==== Status.InternalServerError
      response.contentString must contain(ErrorCode.Internal.desc)
      response.contentString must contain(ex.toString)
    }

    "translates Finagle cancelled request exceptions into Twirp canceled" in new Context {
      val request = httpRequest()
      val ex      = new CancelledRequestException()
      rpc.apply.expects(TestMessage()).once().returning(Future.exception(ex))

      val response = Await.result(server(request))

      response.status ==== Status.RequestTimeout
      response.contentString must contain(ErrorCode.Canceled.desc)
      response.contentString must contain("Request canceled by client")
    }

    "translates Finagle failures caused by cancelled requests into Twirp canceled" in new Context {
      val request = httpRequest()
      val ex      = Failure(new CancelledRequestException())
      rpc.apply.expects(TestMessage()).once().returning(Future.exception(ex))

      val response = Await.result(server(request))

      response.status ==== Status.RequestTimeout
      response.contentString must contain(ErrorCode.Canceled.desc)
      response.contentString must contain("Request canceled by client")
    }
  }
}
