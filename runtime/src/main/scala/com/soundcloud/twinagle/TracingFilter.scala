package com.soundcloud.twinagle

import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.tracing.{ClientTracingFilter, ServerTracingFilter, Trace}
import com.twitter.util.Future

case class TracingFilter(endpointMetadata: EndpointMetadata, isClient: Boolean) extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val trace = Trace()
    trace.recordBinary(TracingFilter.Kind, if (isClient) "client" else "server")
    trace.recordBinary(TracingFilter.Prefix, endpointMetadata.prefix)
    trace.recordBinary(TracingFilter.Service, endpointMetadata.service)
    trace.recordBinary(TracingFilter.Rpc, endpointMetadata.rpc)

    service(request).onSuccess { response =>
    }.onFailure { ex =>
      trace.recordBinary(TracingFilter.Error, true)

      if (ex.isInstanceOf[TwinagleException]) {
        val tex = ex.asInstanceOf[TwinagleException]
        trace.recordBinary(TracingFilter.ErrorCode, tex.code.toString)
        trace.recordBinary(TracingFilter.ErrorMessage, tex.msg)
      }
    }
  }
}

object TracingFilter {
  val Service = "twirp.service"
  val Prefix = "twirp.prefix"
  val Rpc = "twirp.rpc"

  val Kind = "twirp.kind" // whether this is a server or client trace
  val Error = "twirp.is_error"
  val ErrorCode = "twirp.error_code"
  val ErrorMessage = "twirp.error_message"
}
