package proto.test

import com.twitter.util.{Await, Future}
import com.soundcloud.twinagle.ServerBuilder

import org.specs2.mutable.Specification

class MultiServiceSpec extends Specification {
  
  val svc1 = new Service1Service {
    override def rpc1(req: Request): Future[Response] = Future.value(Response())
  }
  
  val svc2 = new Service2Service {
    override def rpc2(req: Request): Future[Response] = Future.value(Response())
  }



  "ServerBuilder allows building services that support multiple Protobuf services" in {
    val httpService = ServerBuilder()
      .register(Service1Service.protoService(svc1))
      .register(Service2Service.protoService(svc2))
      .build

    val svc1Client = new Service1ClientProtobuf(httpService)
    val svc2Client = new Service2ClientProtobuf(httpService) 

    Await.result(svc1Client.rpc1(Request())) ==== Response()
    Await.result(svc2Client.rpc2(Request())) ==== Response()
  }
}
