package com.soundcloud.twinagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ProtobufClientFilter[Req <: GeneratedMessage, Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](path: String) extends Filter[Req, Rep, Request, Response] {
  private val converter = new ProtobufConverter

  override def apply(request: Req, service: Service[Request, Response]): Future[Rep] = {
    val ev = implicitly[GeneratedMessageCompanion[Rep]]
    val httpRequest = converter.mkRequest(path, request)
    service(httpRequest).map(converter.fromResponse(_)(ev))
  }
}
