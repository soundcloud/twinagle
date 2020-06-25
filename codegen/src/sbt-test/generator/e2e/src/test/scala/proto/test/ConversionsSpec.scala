package proto.test

import org.specs2.mutable.Specification

class ConversionsSpec extends Specification {
  
  "customized options work" in {
    val req = Request()
    Request.fromJavaProto(Request.toJavaProto(req)) ==== req
  }
}
