package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}

case class ServerBuilder(
    extension: EndpointMetadata => Filter.TypeAgnostic = _ => Filter.TypeAgnostic.Identity,
    endpoints: Seq[ProtoRpc] = Seq.empty,
    prefix: String = "/twirp"
) {

  require(prefix.startsWith("/"), "prefix must start with slash")
  require(!prefix.endsWith("/"), "prefix must not end with slash")

  def register[T: AsProtoService](svc: T): ServerBuilder = {
    val protoService = implicitly[AsProtoService[T]].asProtoService(svc)
    this.copy(endpoints = endpoints ++ protoService.rpcs)
  }

  def withPrefix(prefix: String): ServerBuilder = {
    this.copy(prefix = prefix)
  }

  def build: Service[Request, Response] =
    new Server(endpoints.map(instrument), prefix)

  private def instrument(rpc: ProtoRpc): ProtoRpc =
    rpc.copy(
      svc = extension(rpc.metadata).toFilter andThen
        new TracingFilter[Request, Response](rpc.metadata) andThen
        rpc.svc
    )

}

trait AsProtoService[T] {
  def asProtoService(t: T): ProtoService
}
