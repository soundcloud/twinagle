package twitch.twirp.example.haberdasher.demo

import com.soundcloud.twinagle.{ErrorCode, TwinagleException}
import com.twitter.finagle.Service
import com.twitter.finagle.http
import com.twitter.util.{Await, Future, Throw}
import org.scalatest.{FlatSpec, MustMatchers}

class HaberdasherSpec extends FlatSpec with MustMatchers {

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


  val httpService: Service[http.Request, http.Response] = HaberdasherService.server(svc)


  "ClientJson" should "make a valid HTTP request" in {

    val client = new HaberdasherClientJson(httpService)

    val hat = Await.result(client.makeHat(Size(12)))

    hat.color === "brown"
    hat.size === 12

  }

  it should "produce TwinagleExceptions for error responses" in {

    val client = new HaberdasherClientJson(httpService)

    val Throw(ex: TwinagleException) = Await.result(client.makeHat(Size(-1)).liftToTry)

    ex.code === ErrorCode.InvalidArgument
  }

  "ClientProtobuf" should "make a valid HTTP request" in {

    val client = new HaberdasherClientProtobuf(httpService)

    val hat = Await.result(client.makeHat(Size(12)))

    hat.color === "brown"
    hat.size === 12

  }

  it should "produce TwinagleExceptions for error responses" in {

    val client = new HaberdasherClientJson(httpService)

    val Throw(ex: TwinagleException) = Await.result(client.makeHat(Size(-1)).liftToTry)

    ex.code === ErrorCode.InvalidArgument
  }
}