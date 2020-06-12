package com.soundcloud.twinagle

/**
  * Endpoint represents a Twirp RPC endpoint.
  *
  * @param service absolute name of the Twirp service.
  * @param rpc     name of the RPC endpoint within the Twirp service.
  */
case class EndpointMetadata(service: String, rpc: String) {

  /** @return the (absolute) HTTP path of the RPC endpoint. */
  def path = s"/twirp/$service/$rpc"
}
