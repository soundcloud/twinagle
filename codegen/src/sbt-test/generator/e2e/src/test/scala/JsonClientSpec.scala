package mygen

import com.twitter.util.Future
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import mygen.protos.demo._
import com.soundcloud.twinagle._
import org.scalatest._

class JsonClientSpec extends FlatSpec with MustMatchers {
  "JsonClient" should "print something out" in {
    val printer: Service[Request, Response] = (req: Request) => {
      println(req.contentString)
      Future.value(Response(req))
    }
    new SomeJsonClient(printer).foo(FooReq("asdfasdf"))
  }
}


// generated
class SomeJsonClient(service: Service[Request, Response]) extends SomeService {

  override def foo(in: FooReq): Future[FooResp] = {
    implicit val companion = FooResp.messageCompanion
    val converter = new JsonConverter()
    val request = converter.mkRequest("/twirp/my.service.SomeService/Foo", in)
    service(request).map(x => converter.mkResponse(x)(companion))
  }
  override def bar(in: BarReq): Future[BarResp] = {
    implicit val companion = BarResp.messageCompanion
    val converter = new JsonConverter()
    val request = converter.mkRequest("/twirp/my.service.SomeService/Bar", in)
    service(request).map(x => converter.mkResponse(x)(companion))
  }
}
