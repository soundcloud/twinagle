package com.soundcloud.twinagle

import org.specs2.matcher.ResultMatchers.beFailing
import org.specs2.mutable.Specification

class TwinagleExceptionSpec extends Specification {

  "TwinagleException when seen as a RuntimeException, message should contain the error code" in {
    val twinagleException = TwinagleException(ErrorCode.Internal, "something went wrong")
    twinagleException.getMessage ==== "TwinagleException with errorCode internal: something went wrong"
  }
  "specs2 throwsA(someTwinagleException) fails if someTwinagleException has different error code" in {
    val internalTwinagleException = TwinagleException(ErrorCode.Internal, "my error message")
    val expectedException         = TwinagleException(ErrorCode.NotFound, "my error message")
    def myFunction(): Unit        = throw internalTwinagleException
    (myFunction() must throwA(expectedException)) must beFailing
  }

}
