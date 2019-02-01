package com.soundcloud.twinagle

import com.twitter.finagle.http.{MediaType, Method, Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import scalapb.json4s.JsonFormat
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class JsonClientFilter[Req <: GeneratedMessage, Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](path: String) extends Filter[Req, Rep, Request, Response] {

  override def apply(request: Req, service: Service[Request, Response]): Future[Rep] = {
    val httpRequest = serializeRequest(path, request)
    service(httpRequest).map(deserializeResponse)
  }

  def serializeRequest(path: String, r: Req): Request = {
    val request = Request(Method.Post, path)
    request.contentType = MediaType.Json
    request.contentString = JsonFormat.toJsonString(r)
    request
  }

  def deserializeResponse(response: Response): Rep = {
    JsonFormat.fromJsonString[Rep](response.contentString)
  }

}
