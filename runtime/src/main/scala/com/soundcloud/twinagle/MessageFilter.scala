
import com.twitter.finagle.Filter
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

trait MessageFilter {
  def toFilter[
      Req <: GeneratedMessage: GeneratedMessageCompanion,
      Resp <: GeneratedMessage: GeneratedMessageCompanion
  ]: Filter[Req, Resp, Req, Resp]

  def andThen(other: MessageFilter): MessageFilter = new MessageFilter {
    override def toFilter[
        Req <: GeneratedMessage: GeneratedMessageCompanion,
        Resp <: GeneratedMessage: GeneratedMessageCompanion
    ]: Filter[Req, Resp, Req, Resp] = this.toFilter[Req, Resp] andThen other.toFilter[Req, Resp]
  }
}

object MessageFilter {
  object Identity extends MessageFilter {
    override def toFilter[
        Req <: GeneratedMessage: GeneratedMessageCompanion,
        Resp <: GeneratedMessage: GeneratedMessageCompanion
    ]: Filter[Req, Resp, Req, Resp] =
      Filter.identity

    override def andThen(other: MessageFilter): MessageFilter = other
  }
}
