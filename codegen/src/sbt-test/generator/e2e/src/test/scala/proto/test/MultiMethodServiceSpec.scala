package proto.test

import com.twitter.util.Future
import com.soundcloud.twinagle.ServerBuilder

import org.specs2.mutable.Specification

class MultiMethodServiceSpec extends Specification {

  // if this compiles, we're good
  "ServerBuilder allows building services that contain multiple RPCs" in {
    val svc = new MultiMethodService {
      override def rpc1(req: MMRequest): Future[MMResponse] = Future.value(MMResponse())
      override def rpc2(req: MMRequest): Future[MMResponse] = Future.value(MMResponse())
    }


    val httpService = ServerBuilder()
      .register(svc)
      .build

    new MultiMethodClientProtobuf(httpService)
    new MultiMethodClientJson(httpService)
    ok
  }
}
