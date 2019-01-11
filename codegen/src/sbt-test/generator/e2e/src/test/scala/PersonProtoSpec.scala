package mygen

import com.twitter.util.Future
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import mygen.protos.demo._
import com.soundcloud.twinagle._
import org.scalatest._

class PersonProtoSpec extends FlatSpec with MustMatchers {
  "PersonBoo" should "have correct field count" in {
    PersonBoo.FieldCount must be(2)
  }
}

// generated
trait SomeService {
  def foo(in: FooReq): Future[FooResp]

  def bar(in: BarReq): Future[BarResp]
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
