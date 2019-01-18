package com.soundcloud.twinagle

import com.twitter.finagle.http.{MediaType, Request, Response}
import scalapb.json4s.JsonFormat
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

// runtime
class JsonConverter {
  def mkRequest(path: String, r: GeneratedMessage): Request = {
    val request = Request(path)
    request.contentType = MediaType.Json
    request.contentString = JsonFormat.toJsonString(r)
    request
  }

  def mkResponse[Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](response: Response): Rep = {
    JsonFormat.fromJsonString[Rep](response.contentString)
  }
}
