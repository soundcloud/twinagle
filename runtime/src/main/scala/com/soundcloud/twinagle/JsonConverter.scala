package com.soundcloud.twinagle

import com.twitter.finagle.http.{MediaType, Method, Request, Response}
import scalapb.json4s.JsonFormat
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

// runtime
class JsonConverter {
  // TODO: we need some kind of baseUrl
  def mkRequest(path: String, r: GeneratedMessage): Request = {
    val request = Request(Method.Post, path)
    request.contentType = MediaType.Json
    request.contentString = JsonFormat.toJsonString(r)
    request
  }

  def fromResponse[Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](response: Response): Rep = {
    JsonFormat.fromJsonString[Rep](response.contentString)
  }
}
