package com.soundcloud.twinagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}

/**
  * Endpoint represents a Twirp RPC endpoint.
  * @param path the (absolute) HTTP path of the RPC endpoint.
  * @param service a service that handles POST requests (JSON or Protobuf body, determined by the request Content-Type)
  */
case class Endpoint(path: String, service: Service[Request, Response])
