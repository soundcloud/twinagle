package twitch.twirp.example.haberdasher.haberdasher

import com.soundcloud.twinagle.{ErrorCode, TwinagleException}
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
        Future.exception(
          TwinagleException(ErrorCode.InvalidArgument, "size must be positive"))
      }
  }

  val faultySvc = new HaberdasherService {
    var shouldFail = true
    override def makeHat(size: Size): Future[Hat] =
      if (shouldFail) {
        shouldFail = false
        Future.exception(
          TwinagleException(ErrorCode.InvalidArgument,
                            "failing because I'm flaky!"))
      } else if (size.inches >= 0) {
        Future.value(
          Hat(
            size = size.inches,
            color = "brown",
            name = "bowler"
          )
        )
      } else {
        Future.exception(
          TwinagleException(ErrorCode.InvalidArgument, "size must be positive"))
      }
  }

  val httpService: Service[http.Request, http.Response] =
    HaberdasherService.server(svc)

  "ClientJson" >> {

    "make a valid HTTP request" in {
      val client = new HaberdasherClientJson(httpService)
      val hat = Await.result(client.makeHat(Size(12)))

      hat.color ==== "brown"
      hat.size ==== 12
    }

    "produces TwinagleExceptions for error responses" in {
      val client = new HaberdasherClientJson(httpService)
      val Throw(ex: TwinagleException) =
        Await.result(client.makeHat(Size(-1)).liftToTry)

      ex.code ==== ErrorCode.InvalidArgument
    }

    "retries idempotent methods" in {
      val faultyHttpService: Service[http.Request, http.Response] =
        HaberdasherService.server(faultySvc)

      val client = new HaberdasherClientJson(faultyHttpService)

      val hat = Await.result(client.makeHat(Size(12)))
      hat.color ==== "brown"
      hat.size ==== 12
    }
  }

  "ClientProtobuf" >> {

    "make a valid HTTP request" in {
      val client = new HaberdasherClientProtobuf(httpService)
      val hat = Await.result(client.makeHat(Size(12)))

      hat.color ==== "brown"
      hat.size ==== 12
    }

    "produces TwinagleExceptions for error responses" in {
      val client = new HaberdasherClientProtobuf(httpService)
      val Throw(ex: TwinagleException) =
        Await.result(client.makeHat(Size(-1)).liftToTry)

      ex.code ==== ErrorCode.InvalidArgument
    }

    "retries idempotent methods" in {

      val faultyHttpService: Service[http.Request, http.Response] =
        HaberdasherService.server(faultySvc)

      val client = new HaberdasherClientProtobuf(faultyHttpService)

      val hat = Await.result(client.makeHat(Size(12)))
      hat.color ==== "brown"
      hat.size ==== 12
    }
  }
}
