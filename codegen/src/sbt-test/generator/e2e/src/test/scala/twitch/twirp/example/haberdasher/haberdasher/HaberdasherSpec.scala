package twitch.twirp.example.haberdasher.haberdasher

import com.soundcloud.twinagle.{ErrorCode, TwinagleException}
import com.twitter.finagle.{Service, http}
import com.twitter.util.{Await, Future, Throw}
import org.specs2.mutable.Specification

class HaberdasherSpec extends Specification {

  def haderdash(size: Size): Future[Hat] = {
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

  val svc = new HaberdasherService {
    override def makeHat(size: Size): Future[Hat] = haderdash(size)
  }

  val flakySvc = new HaberdasherService {
    var shouldFail = true
    override def makeHat(size: Size): Future[Hat] = {
      if (shouldFail) {
        shouldFail = false
        Future.exception(
          TwinagleException(ErrorCode.Unknown, "failing because I'm flaky!"))
      } else {
        haderdash(size)
      }
    }
  }

  val httpService: Service[http.Request, http.Response] =
    HaberdasherService.server(svc)

  val retry: Option[PartialFunction[TwinagleException, Boolean]] = Some({
    case _ => true
  })

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
        HaberdasherService.server(flakySvc)

      val client =
        new HaberdasherClientJson(faultyHttpService, retryMatcher = retry)

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
        HaberdasherService.server(flakySvc)

      val client =
        new HaberdasherClientProtobuf(faultyHttpService, retryMatcher = retry)

      val hat = Await.result(client.makeHat(Size(12)))
      hat.color ==== "brown"
      hat.size ==== 12
    }
  }
}
