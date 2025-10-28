package twitch.twirp.example.haberdasher

import com.twitter.finagle.{Service, http}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification

class HaberdasherSpec extends Specification {

  val svc = new HaberdasherService {
    override def makeHat(size: Size): Future[Hat] =
      if (size.inches >= 0) {
        Future.value(
          Hat(
            size = size.inches,
            color = "brown",
            name = "bowler"
          )
        )
      } else {
        Future.exception(TwinagleException(ErrorCode.InvalidArgument, "size must be positive"))
      }
  }

  "ClientJson" >> {

    "basic usage" >> {
      val httpService: Service[http.Request, http.Response] = HaberdasherService.server(svc)
      val client                                            = new HaberdasherClientJson(httpService)

      "make a valid HTTP request" in {
        val hat = Await.result(client.makeHat(Size(12)))

        hat.color ==== "brown"
        hat.size ==== 12
      }

      "produces TwinagleExceptions for error responses" in {
        val Throw(ex: TwinagleException) = Await.result(client.makeHat(Size(-1)).liftToTry)

        ex.code ==== ErrorCode.InvalidArgument
      }
    }

  }

  "ClientProtobuf" >> {

    "basic usage" >> {
      val httpService: Service[http.Request, http.Response] = HaberdasherService.server(svc)
      val client                                            = new HaberdasherClientProtobuf(httpService)

      "make a valid HTTP request" in {

        val hat = Await.result(client.makeHat(Size(12)))

        hat.color ==== "brown"
        hat.size ==== 12
      }

      "produces TwinagleExceptions for error responses" in {
        val Throw(ex: TwinagleException) = Await.result(client.makeHat(Size(-1)).liftToTry)

        ex.code ==== ErrorCode.InvalidArgument
      }
    }

    "custom prefix" >> {
      val httpService = ServerBuilder().withPrefix("/foo").register(svc).build
      val client      = new HaberdasherClientProtobuf(httpService, prefix = "/foo")

      "make a valid HTTP request" in {

        val hat = Await.result(client.makeHat(Size(12)))

        hat.color ==== "brown"
        hat.size ==== 12
      }

      "produces TwinagleExceptions for error responses" in {
        val Throw(ex: TwinagleException) = Await.result(client.makeHat(Size(-1)).liftToTry)

        ex.code ==== ErrorCode.InvalidArgument
      }
    }
  }
}
