package com.soundcloud.twinagle

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.RetryPolicy.RetryableWriteException
import com.twitter.finagle.service._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.{Throw, Try}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.util.control.NonFatal

class ClientEndpointBuilder(
    httpClient: Service[Request, Response],
    extension: EndpointMetadata => Filter.TypeAgnostic = _ => Filter.TypeAgnostic.Identity
) {

  def jsonEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage with Message[Resp]: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata
  ): Service[Req, Resp] = {

    val retryFilter: Filter[Req, Resp, Req, Resp] = buildRetryFilter(endpointMetadata.isIdempotent)

    extension(endpointMetadata).toFilter andThen
      retryFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new JsonClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      httpClient
  }

  def protoEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage with Message[Resp]: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata
  ): Service[Req, Resp] = {
    val retryFilter: Filter[Req, Resp, Req, Resp] = buildRetryFilter(endpointMetadata.isIdempotent)
    extension(endpointMetadata).toFilter andThen
      retryFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new ProtobufClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      httpClient

  }

  private def buildRetryFilter[Req, Resp](isIdempotent: Boolean) = { // TODO: Are these values reasonable? How configurable should they be?
    val retryPolicy = if (isIdempotent) shouldRetry[Req, Resp] else defaultRetry
    new RetryFilter[Req, Resp](
      RetryPolicy.tries(3, retryPolicy),
      HighResTimer.Default,
      new NullStatsReceiver,
      RetryBudget(60.seconds, 10, 0.2f)
    )
  }

  // Retries all NonFatal exceptions
  // TODO: Make a more informed decision about retrying
  private def shouldRetry[Req, Resp]: PartialFunction[(Req, Try[Resp]), Boolean] = {
    case (_, Throw(t)) => NonFatal(t)
    case _             => false
  }

  private def defaultRetry[Req, Resp]: PartialFunction[(Req, Try[Resp]), Boolean] = {
    case (_, Throw(RetryableWriteException(_))) => true
    case _                                      => false
  }

}
