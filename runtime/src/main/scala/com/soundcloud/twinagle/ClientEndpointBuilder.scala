package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class ClientEndpointBuilder(
    httpClient: Service[Request, Response],
    extension: EndpointMetadata => Filter.TypeAgnostic = _ => Filter.TypeAgnostic.Identity
) {
  def jsonEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata
  ): Service[Req, Resp] = {
    extension(endpointMetadata).toFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new JsonClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      httpClient
  }

  def protoEndpoint[
      Req <: GeneratedMessage,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](
      endpointMetadata: EndpointMetadata
  ): Service[Req, Resp] = {
    extension(endpointMetadata).toFilter andThen
      new TracingFilter[Req, Resp](endpointMetadata) andThen
      new ProtobufClientFilter[Req, Resp](endpointMetadata.path) andThen
      new TwirpHttpClient andThen
      httpClient
  }
}
