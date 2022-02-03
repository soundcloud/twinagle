package proto.test

import com.twitter.util.Future
import com.soundcloud.twinagle.ServerBuilder

import org.specs2.mutable.Specification

class OptionalFieldsSpec extends Specification {
  // if this compiles, we're good
  "Proto files with optional scalar values are generated correctly" in {
    val svc = new OptionalFieldsTestService {
      override def doSomething(req: IHaveOptionalFields): Future[IHaveOptionalFields] = {
        iTakeAnOptionInt(req.maybeInt)
        iTakeAnOptionString(req.maybeString)
        Future.value(req)
      }

      private def iTakeAnOptionInt(a: Option[Int]): Unit = ()

      private def iTakeAnOptionString(a: Option[String]): Unit = ()
    }

    val httpService = ServerBuilder()
      .register(svc)
      .build

    new OptionalFieldsTestClientProtobuf(httpService)
    new OptionalFieldsTestClientJson(httpService)
    ok
  }
}
