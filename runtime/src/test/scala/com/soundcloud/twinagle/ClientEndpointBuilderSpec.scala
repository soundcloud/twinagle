package com.soundcloud.twinagle

import com.twitter.util.Future
import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Request, Response}
import org.specs2.mutable.Specification

class ClientEndpointBuilderSpec extends Specification {

  val httpClient: Service[Request, Response] =
    Service.mk(_ => Future.value(Response()))
  val extension: EndpointMetadata => Filter.TypeAgnostic =
    _ => Filter.TypeAgnostic.Identity

  "prefix validation" >> {

    "default works" in {
      new ClientEndpointBuilder(httpClient, extension, "/twirp") must not(throwAn[IllegalArgumentException])
    }

    "custom path works" in {
      new ClientEndpointBuilder(httpClient, extension, prefix = "/somewhere/else") must not(
        throwAn[IllegalArgumentException]
      )
    }

    "empty prefix" in {
      new ClientEndpointBuilder(httpClient, extension, prefix = "") must not(throwAn[IllegalArgumentException])
    }

    "invalid cases" >> {
      "/ (use empty string)" in {
        new ClientEndpointBuilder(httpClient, extension, prefix = "/") must throwAn[IllegalArgumentException]
      }

      "relative path" in {
        new ClientEndpointBuilder(httpClient, extension, prefix = "relative/path") must throwAn[
          IllegalArgumentException
        ]
      }

      "ends with slash" in {
        new ClientEndpointBuilder(httpClient, extension, prefix = "/some/path/") must throwAn[IllegalArgumentException]
      }
    }

  }
}
