package com.soundcloud.twinagle

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

case class ProtoService(rpcs: Seq[ProtoRpcBuilder])

object ProtoService {
  implicit val asProtoService: AsProtoService[ProtoService] = (t: ProtoService) => t
}

case class ProtoRpc(metadata: EndpointMetadata, svc: Service[Request, Response])

trait ProtoRpcBuilder {
  def build(generatedMessageFilters: Seq[MessageFilter]): ProtoRpc
}

object ProtoRpcBuilder {
  def apply[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](endpointMetadata: EndpointMetadata, rpc: Req => Future[Resp]): ProtoRpcBuilder =
    (generatedMessageFilters: Seq[MessageFilter]) => {
      val twirpFilter: Filter[Request, Response, Req, Resp] = new TwirpEndpointFilter[Req, Resp]
      val svc = generatedMessageFilters.foldLeft(twirpFilter) { case (accFilter, nextFilter) =>
        accFilter.andThen(nextFilter.toFilter)
      } andThen Service.mk(rpc)
      ProtoRpc(endpointMetadata, svc)
    }
}
