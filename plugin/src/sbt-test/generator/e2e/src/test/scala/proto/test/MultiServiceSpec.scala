package proto.test

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.{Await, Future}
import com.soundcloud.twinagle.{MessageFilter, ServerBuilder}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.collection.mutable.ListBuffer

class MultiServiceSpec extends Specification {

  val svc1 = new Service1Service {
    override def rpc1(req: Request): Future[Response] = Future.value(Response())
  }

  val svc2 = new Service2Service {
    override def rpc2(req: Request): Future[Response] = Future.value(Response())
  }

  "ServerBuilder allows building services that support multiple Protobuf services" in {
    val httpService = ServerBuilder()
      .register(svc1)
      .register(svc2)
      .build

    val svc1Client = new Service1ClientProtobuf(httpService)
    val svc2Client = new Service2ClientProtobuf(httpService)

    Await.result(svc1Client.rpc1(Request())) ==== Response()
    Await.result(svc2Client.rpc2(Request())) ==== Response()
  }

  "ServerBuilder with specified message filter" >> {
    trait MessageFilterContext extends Scope {
      val recorderRequests = ListBuffer[GeneratedMessage]()
      val filter = new MessageFilter {
        override def toFilter[
            Req <: GeneratedMessage: GeneratedMessageCompanion,
            Resp <: GeneratedMessage: GeneratedMessageCompanion
        ]: Filter[Req, Resp, Req, Resp] =
          (request: Req, next: Service[Req, Resp]) => {
            recorderRequests += request
            next(request)
          }
      }

      val httpService = ServerBuilder()
        .register(svc1)
        .withMessageFilter(filter)
        .build

      val svc1Client = new Service1ClientProtobuf(httpService)
    }

    "should apply filter for each request" in new MessageFilterContext {
      Await.result(svc1Client.rpc1(Request())) ==== Response()
      recorderRequests.toList ==== List(Request())
    }
  }
}
