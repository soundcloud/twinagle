package com.soundcloud.twinagle

/**
  * Endpoint represents a Twirp RPC endpoint.
  *
  * @param prefix  prefix of all twirp services. Usually, "/twirp".
  * @param service absolute name of the Twirp service.
  * @param rpc     name of the RPC endpoint within the Twirp service.
  */
case class EndpointMetadata(prefix: String, service: String, rpc: String, isIdempotent: Boolean = false) {
  require(prefix.startsWith("/"))
  require(!prefix.endsWith("/"))

  /** @return the (absolute) HTTP path of the RPC endpoint. */
  def path = s"$prefix/$service/$rpc"
}
