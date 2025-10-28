
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Status}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class TwirpEndpointFilterSpec extends Specification {
  "content-type header" >> {
    trait Context extends Scope {
      val svc = new TwirpEndpointFilter[TestMessage, TestMessage] andThen
        Service.mk[TestMessage, TestMessage](msg => Future.value(msg))
    }

    "protobuf" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf"
        // empty body because TestMessage has no fields and serializes as empty content

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/protobuf; charset=UTF-8"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
      }
    }

    "json" >> {
      "with charset" in new Context {
        val request = Request()
        request.contentType = "application/json"
        request.contentString = "{}"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }

      "without charset" in new Context {
        val request = Request()
        request.contentType = "application/json; charset=UTF-8"
        request.contentString = "{}"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }

      "serializes default values" in {
        val svc = new TwirpEndpointFilter[HasField, HasField] andThen
          Service.mk[HasField, HasField](msg => Future.value(msg))

        val request = Request()
        request.contentType = "application/json; charset=UTF-8"
        request.contentString = "{}"

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== """{"foo":0}"""
      }

      "ignores unknown fields" in new Context {
        val request = Request()
        request.contentType = "application/json; charset=UTF-8"
        request.contentString = """{"foo": 123}"""

        val response = Await.result(svc(request))

        response.status ==== Status.Ok
        response.contentString ==== "{}"
      }
    }

    "un-supported content-type" in new Context {
      val request = Request()
      request.contentType = "application/xml; charset=UTF-8"
      request.contentString = "{}"

      Await.result(svc(request).liftToTry) match {
        case Throw(ex: TwinagleException) =>
          ex.code ==== ErrorCode.BadRoute
        case _ => ko
      }
    }

    "unspecified content-type" in new Context {
      val request = Request()
      request.contentString = "{}"

      Await.result(svc(request).liftToTry) match {
        case Throw(ex: TwinagleException) =>
          ex.code ==== ErrorCode.BadRoute
        case _ => ko
      }
    }
  }
}
