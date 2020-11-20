package com.soundcloud.twinagle

import org.specs2.mutable.Specification
import scalapb.json4s.{JsonFormat, Parser}

class UnknownFieldsSpec extends Specification {

  "binary protobuf propagates unknown fields" in {
    val original = Test2(foo = 1, bar = 2)

    val intermediate = Test1.parseFrom(original.toByteArray)
    intermediate.foo ==== original.foo

    val roundtripped = Test2.parseFrom(intermediate.toByteArray)
    roundtripped ==== original
  }

  "JSON parsing ignores unknown fields" in {
    val original = Test2(foo = 1, bar = 2)

    val parser = new Parser().ignoringUnknownFields

    val intermediate = parser.fromJsonString[Test1](JsonFormat.toJsonString(original))
    intermediate.foo ==== original.foo

    val roundtripped = parser.fromJsonString[Test2](JsonFormat.toJsonString(intermediate))
    roundtripped ==== original.copy(bar = 0) // unknown fields are ignored
  }

}
