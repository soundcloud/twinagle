package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

case class ProtoService(rpcs: Seq[ProtoRpc]) {
  assert(rpcs.map(_.metadata.service).toSet.size == 1, "inconsistent services in metadata")
  assert(rpcs.map(_.metadata.prefix).toSet.size == 1, "inconsistent prefixes in metadata")
}

case class ProtoRpc(metadata: EndpointMetadata, svc: Service[Request, Response])
object ProtoRpc {
  def apply[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](endpointMetadata: EndpointMetadata, rpc: Req => Future[Resp]): ProtoRpc = {
    val httpService = new TwirpEndpointFilter[Req, Resp] andThen Service.mk(rpc)
    ProtoRpc(endpointMetadata, httpService)
  }
}
