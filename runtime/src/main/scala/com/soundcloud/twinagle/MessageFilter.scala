package com.soundcloud.twinagle

import com.twitter.finagle.Filter
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

trait MessageFilter {
  def toFilter[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ]: Filter[Req, Resp, Req, Resp]
}
