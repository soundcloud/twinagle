package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

case class ServerBuilder(
    extension: EndpointMetadata => Filter.TypeAgnostic,
    endpoints: Map[EndpointMetadata, Service[Request, Response]] = Map.empty
) {

  def register[
      Req <: GeneratedMessage with Message[Req]: GeneratedMessageCompanion,
      Resp <: GeneratedMessage with Message[Resp]: GeneratedMessageCompanion
  ](endpointMetadata: EndpointMetadata, rpc: Req => Future[Resp]): ServerBuilder = {
    val httpService =
      extension(endpointMetadata).toFilter andThen
        new TracingFilter[Request, Response](endpointMetadata) andThen
        new TwirpEndpointFilter[Req, Resp] andThen
        Service.mk(rpc)
    this.copy(endpoints = endpoints + (endpointMetadata -> httpService))
  }

  def build: Service[Request, Response] = new Server(endpoints)
}
