package com.soundcloud.twinagle

import com.twitter.finagle.http.Status

sealed abstract class ErrorCode(val status: Status, val desc: String)

object ErrorCode {

  //  TODO:  finagle flags maybe?
  /** The operation was cancelled. */
  final case object Canceled extends ErrorCode(Status.RequestTimeout, "canceled")

  /** An unknown error occurred. For example, this can be used when handling errors raised by APIs that do not return any error information. */
  final case object Unknown extends ErrorCode(Status.InternalServerError, "unknown")

  /** The client specified an invalid argument. This indicates arguments that are invalid regardless of the state of the system (i.e. a malformed file name, required argument, number out of range, etc.). */
  final case object InvalidArgument extends ErrorCode(Status.BadRequest, "invalid_argument")

  /** Operation expired before completion. For operations that change the state of the system, this error may be returned even if the operation has completed successfully (timeout). */
  final case object DeadlineExceeded extends ErrorCode(Status.RequestTimeout, "deadline_exceeded")

  /** Some requested entity was not found. */
  final case object NotFound extends ErrorCode(Status.NotFound, "not_found")

  /** The requested URL path wasn't routable to a Twirp service and method. This is returned by generated server code and should not be returned by application code (use "not_found" or "unimplemented" instead). */
  final case object BadRoute extends ErrorCode(Status.NotFound, "bad_route")

  /** An attempt to create an entity failed because one already exists. */
  final case object AlreadyExists extends ErrorCode(Status.Conflict, "already_exists")

  /** The caller does not have permission to execute the specified operation. It must not be used if the caller cannot be identified (use "unauthenticated" instead). */
  final case object PermissionDenied extends ErrorCode(Status.Forbidden, "permission_denied")

  /** The request does not have valid authentication credentials for the operation. */
  final case object Unauthenticated extends ErrorCode(Status.Unauthorized, "unauthenticated")

  /** Some resource has been exhausted, perhaps a per-user quota, or perhaps the entire file system is out of space. */
  final case object ResourceExhausted extends ErrorCode(Status.Forbidden, "resource_exhausted")

  /** The operation was rejected because the system is not in a state required for the operation's execution. For example, doing an rmdir operation on a directory that is non-empty, or on a non-directory object, or when having conflicting read-modify-write on the same resource. */
  final case object FailedPrecondition extends ErrorCode(Status.PreconditionFailed, "failed_precondition")

  /** The operation was aborted, typically due to a concurrency issue like sequencer check failures, transaction aborts, etc. */
  final case object Aborted extends ErrorCode(Status.Conflict, "aborted")

  /** The operation was attempted past the valid range. For example, seeking or reading past end of a paginated collection. Unlike "invalid_argument", this error indicates a problem that may be fixed if the system state changes (i.e. adding more items to the collection). There is a fair bit of overlap between "failed_precondition" and "out_of_range". We recommend using "out_of_range" (the more specific error) when it applies so that callers who are iterating through a space can easily look for an "out_of_range" error to detect when they are done. */
  final case object OutOfRange extends ErrorCode(Status.BadRequest, "out_of_range")

  /** The operation is not implemented or not supported/enabled in this service. */
  final case object Unimplemented extends ErrorCode(Status.NotImplemented, "unimplemented")

  /** When some invariants expected by the underlying system have been broken. In other words, something bad happened in the library or backend service. Twirp specific issues like wire and serialization problems are also reported as "internal" errors. */
  final case object Internal extends ErrorCode(Status.InternalServerError, "internal")

  /** The service is currently unavailable. This is most likely a transient condition and may be corrected by retrying with a backoff. */
  final case object Unavailable extends ErrorCode(Status.ServiceUnavailable, "unavailable")

  /** The operation resulted in unrecoverable data loss or corruption. */
  final case object Dataloss extends ErrorCode(Status.InternalServerError, "dataloss")
}

case class TwinagleException(code: ErrorCode, msg: String, meta: Map[String, String] = Map.empty, cause: Throwable = null) extends RuntimeException(msg, cause) {
  def this(cause: Throwable) = this(ErrorCode.Internal, cause.toString, Map.empty, cause)
}
