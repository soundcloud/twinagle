package mygen

import com.soundcloud.twinagle.{ErrorCode, TwinagleException}
import com.twitter.util.{Await, Future}
import mygen.protos.demo._
import org.scalatest._

class JsonClientSpec extends FlatSpec with MustMatchers {
  "JsonClient" should "print something out" in {
    
    val svc = new SomeService {
      override def foo(fooReq: FooReq): Future[FooResp] = {
        println(fooReq.asdf)
        Future.value(FooResp())
      }

      override def bar(barReq: BarReq): Future[BarResp] =
        Future.exception(TwinagleException(ErrorCode.AlreadyExists, "whoops!"))
    }

    val httpService = SomeService.server(svc)


    val asdf = new SomeClientJson(httpService).foo(FooReq("asdfasdf"))
    val unit = new SomeClientProtobuf(httpService).foo(FooReq("asdfasdf"))

    val err = new SomeClientJson(httpService).bar(BarReq()).handle {
      case TwinagleException(code, msg, meta, t) => println(s"other: $code")
    }

    Await.result(Future.collect(Seq(asdf, unit, err)))

  }
}
