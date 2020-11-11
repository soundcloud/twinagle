package com.soundcloud.twinagle

import org.specs2.mutable.Specification
import scalapb.json4s.JsonFormat

class UnknownFieldsSpec extends Specification{

  "should propagate unknown fields" in {
    val original = Test2(foo = 1, bar = 2)

    val intermediate = Test1.parseFrom(original.toByteArray)
    intermediate.foo ==== original.foo

    val roundtripped = Test2.parseFrom(intermediate.toByteArray)
    roundtripped ==== original
  }

}
