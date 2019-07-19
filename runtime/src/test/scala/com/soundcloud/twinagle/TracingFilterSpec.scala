package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.tracing.{Annotation, BufferingTracer, Trace}
import com.twitter.util.{Await, Future}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class TracingFilterSpec extends Specification {
  trait Context extends Scope {
    val tracer   = new BufferingTracer
    val response = Response()
    val request  = Request(Method.Post, "/twirp/svc/rpc")

    def binaryAnnotations =
      tracer
        .map(_.annotation)
        .collect {
          case Annotation.BinaryAnnotation(k, v) => (k, v)
        }
        .toMap

  }

  "adds annotations" >> {
    "successful request" in new Context {
      Trace.letTracer(tracer) {
        val svc = new TracingFilter(EndpointMetadata("/twirp", "svc", "rpc", false)) andThen
          Service.const(Future.value(response))
        Await.result(svc(request))
      }

      binaryAnnotations.get(TracingFilter.Prefix) ==== Some("/twirp")
      binaryAnnotations.get(TracingFilter.Service) ==== Some("svc")
      binaryAnnotations.get(TracingFilter.Rpc) ==== Some("rpc")

      binaryAnnotations.get(TracingFilter.Error) ==== None
      binaryAnnotations.get(TracingFilter.ErrorCode) ==== None
      binaryAnnotations.get(TracingFilter.ErrorMessage) ==== None
    }

    "twinagle errors" in new Context {
      val exception = TwinagleException(ErrorCode.NotFound, "foo not found")
      Trace.letTracer(tracer) {
        val svc = new TracingFilter(EndpointMetadata("/twirp", "svc", "rpc", false)) andThen
          Service.const(Future.exception(exception))
        Await.result(svc(request).liftToTry)
      }

      binaryAnnotations.get(TracingFilter.Prefix) ==== Some("/twirp")
      binaryAnnotations.get(TracingFilter.Service) ==== Some("svc")
      binaryAnnotations.get(TracingFilter.Rpc) ==== Some("rpc")

      binaryAnnotations.get(TracingFilter.Error) ==== Some(true)
      binaryAnnotations.get(TracingFilter.ErrorCode) ==== Some("not_found")
      binaryAnnotations.get(TracingFilter.ErrorMessage) ==== Some(
        "foo not found"
      )
    }

    "other errors" in new Context {
      val exception = new RuntimeException("boom")
      Trace.letTracer(tracer) {
        val svc = new TracingFilter(EndpointMetadata("/twirp", "svc", "rpc", false)) andThen
          Service.const(Future.exception(exception))
        Await.result(svc(request).liftToTry)
      }

      binaryAnnotations.get(TracingFilter.Error) ==== Some(true)
      binaryAnnotations.get(TracingFilter.ErrorCode) ==== None
      binaryAnnotations.get(TracingFilter.ErrorMessage) ==== None
    }
  }
}
