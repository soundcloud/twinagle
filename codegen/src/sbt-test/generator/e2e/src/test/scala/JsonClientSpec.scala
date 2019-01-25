package mygen

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import mygen.protos.demo._
import org.scalatest._

class JsonClientSpec extends FlatSpec with MustMatchers {
  "JsonClient" should "print something out" in {
    val printer: Service[Request, Response] = (req: Request) => {
      println(req.contentString)
      Future.value(Response(req))
    }
    new SomeClientJson(printer).foo(FooReq("asdfasdf"))
//    new SomeClientProtobuf(printer).foo(FooReq("asdfasdf"))
  }
}


