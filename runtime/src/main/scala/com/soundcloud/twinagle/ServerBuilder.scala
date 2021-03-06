package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}

/** ServerBuilder can be used to customize the Twinagle HTTP server.
  *
  * In the simple case, you can use `MyService.server()` to create an HTTP server,
  * but if your application needs to satisfy multiple Twirp services or customize
  * the HTTP path prefix, you can use `ServerBuilder` to create an HTTP server instead.
  */
class ServerBuilder private (
    extension: EndpointMetadata => Filter.TypeAgnostic,
    endpoints: Seq[ProtoRpc],
    prefix: String
) {

  if (prefix.nonEmpty) {
    require(prefix.startsWith("/"), "prefix must start with slash")
    require(!prefix.endsWith("/"), "prefix must not end with slash")
  }

  /** register a service with ServerBuilder.
    * All service traits generated by twinagle from Protobuf service definitions
    * can be used with `register`, since they implement the `AsProtoservice` typeclass.
    */
  def register[T: AsProtoService](svc: T): ServerBuilder = {
    val protoService = implicitly[AsProtoService[T]].asProtoService(svc)
    new ServerBuilder(extension, endpoints ++ protoService.rpcs, prefix)
  }

  /** withPrefix configures the HTTP path prefix to use for this server (default: `/twirp`).
    * Paths must be absolute and may not end with `/`.
    * Use an empty string to expose endpoints at the root of the HTTP path.
    */
  def withPrefix(prefix: String): ServerBuilder = {
    new ServerBuilder(extension, endpoints, prefix)
  }

  /** create an HTTP server that implements the Twirp wire protocol by
    * dispatching to the registered services.
    */
  def build: Service[Request, Response] =
    new Server(endpoints.map(instrument), prefix)

  private def instrument(rpc: ProtoRpc): ProtoRpc =
    rpc.copy(
      svc = extension(rpc.metadata).toFilter andThen
        new TracingFilter[Request, Response](rpc.metadata) andThen
        rpc.svc
    )

}

object ServerBuilder {
  def apply(
      extension: EndpointMetadata => Filter.TypeAgnostic = _ => Filter.TypeAgnostic.Identity,
      endpoints: Seq[ProtoRpc] = Seq.empty,
      prefix: String = "/twirp"
  ): ServerBuilder = new ServerBuilder(extension, endpoints, prefix)
}

trait AsProtoService[T] {
  def asProtoService(t: T): ProtoService
}
