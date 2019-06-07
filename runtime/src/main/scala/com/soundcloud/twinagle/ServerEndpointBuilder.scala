package com.soundcloud.twinagle

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ServerEndpointBuilder(
    extension: EndpointMetadata => Filter.TypeAgnostic
) {

  def build[Req <: GeneratedMessage with Message[Req]: GeneratedMessageCompanion, Resp <: GeneratedMessage with Message[
    Resp
  ]: GeneratedMessageCompanion](
      rpc: Req => Future[Resp],
      endpointMetadata: EndpointMetadata
  ): (EndpointMetadata, Service[Request, Response]) = {
    val httpService =
      extension(endpointMetadata).toFilter andThen
        new TracingFilter[Request, Response](endpointMetadata) andThen
        new TwirpEndpointFilter[Req, Resp] andThen
        Service.mk(rpc)
    endpointMetadata -> httpService
  }
}
