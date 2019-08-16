package com.soundcloud.twinagle.session

import com.twitter.finagle.context.{Contexts, LocalContext}
import com.twitter.finagle.http.HeaderMap

object SessionCache {
  final val context: LocalContext                    = Contexts.local
  final val customHeadersKey: context.Key[HeaderMap] = context.newKey[HeaderMap]()
}
