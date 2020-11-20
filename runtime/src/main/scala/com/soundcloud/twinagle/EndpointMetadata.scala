package com.soundcloud.twinagle

/** EndpointMetadata represents a Twirp RPC endpoint.
  *
  * @param service absolute name of the Twirp service.
  * @param rpc     name of the RPC endpoint within the Twirp service.
  */
case class EndpointMetadata(service: String, rpc: String)
