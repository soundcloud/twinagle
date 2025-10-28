package proto.test

import com.twitter.util.Future

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

      @annotation.nowarn
      private def iTakeAnOptionInt[T](a: T)(implicit ev: T =:= Option[Int]): Unit = ()

      @annotation.nowarn
      private def iTakeAnOptionString[T](a: T)(implicit ev: T =:= Option[String]): Unit = ()
    }

    val httpService = ServerBuilder()
      .register(svc)
      .build

    new OptionalFieldsTestClientProtobuf(httpService)
    new OptionalFieldsTestClientJson(httpService)
    ok
  }
}
