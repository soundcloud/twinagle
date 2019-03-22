package com.soundcloud.twinagle

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ServerEndpointBuilder(extension: EndpointMetadata => Filter.TypeAgnostic) {
  // TODO: refactor types to aliases?
  def build[Req <: GeneratedMessage with Message[Req] : GeneratedMessageCompanion, Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](rpc: Req => Future[Rep], endpoint: EndpointMetadata): (EndpointMetadata, Service[Request, Response]) = {
    val httpService: Service[Request, Response] = new ServiceAdapter(rpc)
    endpoint -> extension(endpoint).toFilter.andThen(httpService)
  }
}
