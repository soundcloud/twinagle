package com.soundcloud.twinagle

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.io.Buf
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ProtobufConverter {

  def mkRequest(path: String, r: GeneratedMessage): Request = {
    val request = Request(Method.Post, path)
    request.contentType = "application/protobuf"
    request.content = Buf.ByteArray.Owned(r.toByteArray)
    request
  }

  def fromResponse[Rep <: GeneratedMessage with Message[Rep] : GeneratedMessageCompanion](response: Response): Rep = {
    val companion = implicitly[GeneratedMessageCompanion[Rep]]
    companion.parseFrom(Buf.ByteArray.Owned.extract(response.content))
  }

}
