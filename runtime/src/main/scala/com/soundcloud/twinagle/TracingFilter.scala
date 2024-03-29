package com.soundcloud.twinagle

import com.twitter.finagle._
import com.twitter.finagle.tracing.Trace
import com.twitter.util.Future

private[twinagle] class TracingFilter[In, Out](
    endpointMetadata: EndpointMetadata
) extends SimpleFilter[In, Out] {
  override def apply(request: In, service: Service[In, Out]): Future[Out] = {
    val trace = Trace()
    trace.recordServiceName(endpointMetadata.service)
    trace.recordRpc(endpointMetadata.rpc)
    trace.recordBinary(TracingFilter.Service, endpointMetadata.service)
    trace.recordBinary(TracingFilter.Rpc, endpointMetadata.rpc)

    service(request).onFailure {
      case ex: TwinagleException =>
        trace.recordBinary(TracingFilter.Error, true)
        trace.recordBinary(TracingFilter.ErrorCode, ex.code.toString)
        trace.recordBinary(TracingFilter.ErrorMessage, ex.msg)
      case ex =>
        trace.recordBinary(TracingFilter.Error, true)
    }
  }
}

private[twinagle] object TracingFilter {
  val Service = "twirp.service"
  val Rpc     = "twirp.rpc"

  val Error        = "error"
  val ErrorCode    = "twirp.error_code"
  val ErrorMessage = "twirp.error_message"
}
