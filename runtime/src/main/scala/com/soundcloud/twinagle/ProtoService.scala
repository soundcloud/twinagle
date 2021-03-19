package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

case class ProtoService(rpcs: Seq[ProtoRpcBuilder]) {
  assert(rpcs.map(_.metadata.service).toSet.size == 1, "inconsistent services in metadata")
}

object ProtoService {
  implicit val asProtoService: AsProtoService[ProtoService] = (t: ProtoService) => t
}

case class ProtoRpc(metadata: EndpointMetadata, svc: Service[Request, Response])

object ProtoRpc {
  def apply[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](endpointMetadata: EndpointMetadata, rpc: Req => Future[Resp]): ProtoRpc = {
    ProtoRpcBuilder(endpointMetadata, rpc).build(Seq.empty)
  }
}

trait ProtoRpcBuilder {
  val metadata: EndpointMetadata

  def build(messageFilters: Seq[MessageFilter]): ProtoRpc
}

object ProtoRpcBuilder {
  def apply[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ](endpointMetadata: EndpointMetadata, rpc: Req => Future[Resp]): ProtoRpcBuilder = new ProtoRpcBuilder {
    override val metadata: EndpointMetadata = endpointMetadata

    override def build(messageFilters: Seq[MessageFilter]): ProtoRpc = {
      val svc: Service[Req, Resp] = messageFilters.foldRight(Service.mk(rpc)) { case (messageFilter, accSvc) =>
        messageFilter.toFilter[Req, Resp] andThen accSvc
      }
      ProtoRpc(endpointMetadata, new TwirpEndpointFilter[Req, Resp] andThen svc)
    }
  }
}
