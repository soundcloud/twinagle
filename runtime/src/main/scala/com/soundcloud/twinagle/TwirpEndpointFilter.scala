package com.soundcloud.twinagle

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http._
import com.twitter.finagle.{Filter, Service}
import com.twitter.io.Buf
import com.twitter.util.Future
import scalapb.json4s.{Parser, Printer}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import java.nio.ByteBuffer

/** Decodes Protobuf or Json-encoded HTTP requests into Twirp messages
  *
  * @tparam Req incoming twirp request message
  * @tparam Rep outgoing twirp response message
  */
private[twinagle] class TwirpEndpointFilter[
    Req <: GeneratedMessage: GeneratedMessageCompanion,
    Rep <: GeneratedMessage: GeneratedMessageCompanion
] extends Filter[Request, Response, Req, Rep] {

  private val jsonParser = new Parser().ignoringUnknownFields
  private val printer    = new Printer().includingDefaultValueFields

  override def apply(
      request: Request,
      service: Service[Req, Rep]
  ): Future[Response] = request.mediaType match {
    case Some(MediaType.Json) =>
      val input = jsonParser.fromJsonString[Req](request.contentString)
      service(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = MediaType.Json
        response.contentString = printer.print(r)
        response
      }
    case Some("application/protobuf") =>
      val input = implicitly[GeneratedMessageCompanion[Req]]
        .parseFrom(toBytes(request.content))
      service(input).map { r =>
        val response = Response(Status.Ok)
        response.contentType = "application/protobuf"
        response.content = Buf.ByteArray.Owned(r.toByteArray)
        response
      }
    case Some("application/grpc+proto") =>
      val input = implicitly[GeneratedMessageCompanion[Req]]
        .parseFrom(toBytes(request.content).drop(5))

      // make metadata available to downstream service
      Contexts.local.let(twirpGRPCMetadata, request.headerMap) {
        val response = Response(Status.Ok)
        response.version = Version(2, 0)
        response.contentType = "application/grpc+proto"
        response.setChunked(true)
        val writer = response.chunkWriter

        service(input)
          .map { r =>
            val buffer = ByteBuffer.allocate(4)
            buffer.order(java.nio.ByteOrder.BIG_ENDIAN)
            buffer.putInt(r.toByteArray.length)
            val messageLength = buffer.array()
            // todo: how and when to toggle compression
            val compressed        = 0.byteValue
            val data: Array[Byte] = (compressed +: messageLength) ++ r.toByteArray
            val trailers          = HeaderMap(
              "grpc-status"  -> "0",
              "grpc-message" -> "OK"
            )

            writer.write(Chunk.fromByteArray(data)).ensure {
              writer.write(Chunk.last(trailers)).ensure {
                writer.close()
              }
            }

            response
          }
          .handle { case TwinagleException(code, message, _, _) =>
            val grpcStatus = code match {
              case ErrorCode.Canceled           => io.grpc.Status.CANCELLED
              case ErrorCode.Unknown            => io.grpc.Status.UNKNOWN
              case ErrorCode.InvalidArgument    => io.grpc.Status.INVALID_ARGUMENT
              case ErrorCode.DeadlineExceeded   => io.grpc.Status.DEADLINE_EXCEEDED
              case ErrorCode.NotFound           => io.grpc.Status.NOT_FOUND
              case ErrorCode.BadRoute           => io.grpc.Status.UNKNOWN
              case ErrorCode.AlreadyExists      => io.grpc.Status.ALREADY_EXISTS
              case ErrorCode.PermissionDenied   => io.grpc.Status.PERMISSION_DENIED
              case ErrorCode.Unauthenticated    => io.grpc.Status.UNAUTHENTICATED
              case ErrorCode.ResourceExhausted  => io.grpc.Status.RESOURCE_EXHAUSTED
              case ErrorCode.FailedPrecondition => io.grpc.Status.FAILED_PRECONDITION
              case ErrorCode.Aborted            => io.grpc.Status.ABORTED
              case ErrorCode.OutOfRange         => io.grpc.Status.OUT_OF_RANGE
              case ErrorCode.Unimplemented      => io.grpc.Status.UNIMPLEMENTED
              case ErrorCode.Internal           => io.grpc.Status.INTERNAL
              case ErrorCode.Unavailable        => io.grpc.Status.UNAVAILABLE
              case ErrorCode.Dataloss           => io.grpc.Status.DATA_LOSS
            }

            val trailers = HeaderMap(
              "grpc-status"  -> grpcStatus.getCode.value().toString,
              "grpc-message" -> s"$message [${code.desc}]"
            )

            // errors are trailers-only
            writer.write(Chunk.last(trailers)).ensure {
              writer.close()
            }

            response
          }
      }

    case _ =>
      val contentType = request.contentType.getOrElse("")
      Future.exception(
        TwinagleException(
          ErrorCode.BadRoute,
          s"unexpected Content-Type: '$contentType'"
        )
      )
  }

  private val twirpGRPCMetadata = Contexts.local.newKey[HeaderMap]()

  private def toBytes(buf: Buf): Array[Byte] = Buf.ByteArray.Owned.extract(buf)
}
