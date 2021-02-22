package com.soundcloud.twinagle

import com.twitter.finagle.SimpleFilter
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

trait MessageFilter {
  def toFilter[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ]: SimpleFilter[Req, Resp]
}
