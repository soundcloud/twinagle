package com.soundcloud.twinagle

import org.specs2.mutable.Specification

class ServerBuilderSpec extends Specification {
  "prefix validation" >> {

    "default works" in {
      ServerBuilder() must not(throwAn[IllegalArgumentException])
    }

    "custom path works" in {
      ServerBuilder(prefix = "/somewhere/else") must not(throwAn[IllegalArgumentException])
    }

    "empty prefix" in {
      ServerBuilder(prefix = "") must not(throwAn[IllegalArgumentException])
    }

    "invalid cases" >> {
      "/ (use empty string)" in {
        ServerBuilder(prefix = "/") must throwAn[IllegalArgumentException]
      }

      "relative path" in {
        ServerBuilder(prefix = "relative/path") must throwAn[IllegalArgumentException]
      }

      "ends with slash" in {
        ServerBuilder(prefix = "/some/path/") must throwAn[IllegalArgumentException]
      }

      "copy method validates" in {
        ServerBuilder().copy(prefix = "/invalid/") must throwAn[IllegalArgumentException]
      }

      "withPrefix method validates" in {
        ServerBuilder().withPrefix("/invalid/") must throwAn[IllegalArgumentException]
      }
    }


  }
}
