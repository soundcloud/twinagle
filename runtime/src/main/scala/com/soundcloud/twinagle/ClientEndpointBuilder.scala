package com.soundcloud.twinagle

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.RetryPolicy.RetryableWriteException
import com.twitter.finagle.service._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.{Return, Throw, Try}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ClientEndpointBuilder(
    httpClient: Service[Request, Response],
    extension: EndpointMetadata => Filter.TypeAgnostic = _ => Filter.TypeAgnostic.Identity
) {

  def jsonEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage with Message[Resp]: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata,
      retryMatcher: Option[PartialFunction[TwinagleException, Boolean]]
  ): Service[Req, Resp] = {

    val retryFilter: RetryFilter[Request, Response] = buildHttpRetryFilter(endpointMetadata.isIdempotent, retryMatcher)

    extension(endpointMetadata).toFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new JsonClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      retryFilter andThen
      httpClient
  }

  def protoEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage with Message[Resp]: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata,
      retryMatcher: Option[PartialFunction[TwinagleException, Boolean]]
  ): Service[Req, Resp] = {
    val retryFilter: RetryFilter[Request, Response] = buildHttpRetryFilter(endpointMetadata.isIdempotent, retryMatcher)

    extension(endpointMetadata).toFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new ProtobufClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      retryFilter andThen
      httpClient

  }

  private def buildHttpRetryFilter(isIdempotent: Boolean,
                                   retryMatcher: Option[PartialFunction[TwinagleException, Boolean]]) = {
    new RetryFilter[Request, Response](
      RetryPolicy.tries(3, {
        case (req, resp) if isIdempotent => httpRetryPolicy(retryMatcher)((req, resp))
      }),
      HighResTimer.Default,
      new NullStatsReceiver,
      RetryBudget(60.seconds, 10, 0.2f)
    )
  }

  private def httpRetryPolicy(
      retryMatcher: Option[PartialFunction[TwinagleException, Boolean]]
  ): PartialFunction[(Request, Try[Response]), Boolean] = {
    case (_, response) =>
      (response, retryMatcher) match {
        case (Return(r), Some(f)) =>
          f(TwirpHttpClient.errorFromResponse(r))
        case (Throw(RetryableWriteException(_)), _) => true // No bytes written to the wire, very conservative retry
        case _                                      => false
      }
  }

}
