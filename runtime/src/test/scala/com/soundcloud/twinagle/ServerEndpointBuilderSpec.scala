package com.soundcloud.twinagle

import com.soundcloud.twinagle.test.TestMessage
import com.twitter.finagle.Filter
import com.twitter.finagle.http.{MediaType, Request}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification

class ServerEndpointBuilderSpec extends Specification {

  "catches exceptions thrown by user code" in {
    val ex = new Exception("oops")

    def throwingRpc(x: TestMessage): Future[TestMessage] = {
      throw ex
    }

    val builder = new ServerEndpointBuilder(_ => Filter.TypeAgnostic.Identity)
    val (_, svc) = builder.build(
      throwingRpc,
      EndpointMetadata("/twirp", "service", "rpc")
    )

    val request = Request()
    request.mediaType = MediaType.Json
    request.contentString = "{}"

    Await.result(svc(request).liftToTry) ==== Throw(ex)
  }
}
