package com.soundcloud.twinagle

sealed abstract case class ErrorCode(desc: String) {
  override def toString: String = desc
}

object ErrorCode {

  //  TODO:  finagle flags maybe?
  /** The operation was cancelled. */
  object Canceled extends ErrorCode("canceled")

  /** An unknown error occurred. For example, this can be used when handling errors raised by APIs that do not return any error information. */
  object Unknown extends ErrorCode("unknown")

  /** The client specified an invalid argument. This indicates arguments that are invalid regardless of the state of the system (i.e. a malformed file name, required argument, number out of range, etc.). */
  object InvalidArgument extends ErrorCode("invalid_argument")

  /** Operation expired before completion. For operations that change the state of the system, this error may be returned even if the operation has completed successfully (timeout). */
  object DeadlineExceeded extends ErrorCode("deadline_exceeded")

  /** Some requested entity was not found. */
  object NotFound extends ErrorCode("not_found")

  /** The requested URL path wasn't routable to a Twirp service and method. This is returned by generated server code and should not be returned by application code (use "not_found" or "unimplemented" instead). */
  object BadRoute extends ErrorCode("bad_route")

  /** An attempt to create an entity failed because one already exists. */
  object AlreadyExists extends ErrorCode("already_exists")

  /** The caller does not have permission to execute the specified operation. It must not be used if the caller cannot be identified (use "unauthenticated" instead). */
  object PermissionDenied extends ErrorCode("permission_denied")

  /** The request does not have valid authentication credentials for the operation. */
  object Unauthenticated extends ErrorCode("unauthenticated")

  /** Some resource has been exhausted, perhaps a per-user quota, or perhaps the entire file system is out of space. */
  object ResourceExhausted extends ErrorCode("resource_exhausted")

  /** The operation was rejected because the system is not in a state required for the operation's execution. For example, doing an rmdir operation on a directory that is non-empty, or on a non-directory object, or when having conflicting read-modify-write on the same resource. */
  object FailedPrecondition extends ErrorCode("failed_precondition")

  /** The operation was aborted, typically due to a concurrency issue like sequencer check failures, transaction aborts, etc. */
  object Aborted extends ErrorCode("aborted")

  /** The operation was attempted past the valid range. For example, seeking or reading past end of a paginated collection. Unlike "invalid_argument", this error indicates a problem that may be fixed if the system state changes (i.e. adding more items to the collection). There is a fair bit of overlap between "failed_precondition" and "out_of_range". We recommend using "out_of_range" (the more specific error) when it applies so that callers who are iterating through a space can easily look for an "out_of_range" error to detect when they are done. */
  object OutOfRange extends ErrorCode("out_of_range")

  /** The operation is not implemented or not supported/enabled in this service. */
  object Unimplemented extends ErrorCode("unimplemented")

  /** When some invariants expected by the underlying system have been broken. In other words, something bad happened in the library or backend service. Twirp specific issues like wire and serialization problems are also reported as "internal" errors. */
  object Internal extends ErrorCode("internal")

  /** The service is currently unavailable. This is most likely a transient condition and may be corrected by retrying with a backoff. */
  object Unavailable extends ErrorCode("unavailable")

  /** The operation resulted in unrecoverable data loss or corruption. */
  object Dataloss extends ErrorCode("dataloss")

  def fromString(code: String): Option[ErrorCode] = code match {
    case Canceled.desc           => Some(Canceled)
    case Unknown.desc            => Some(Unknown)
    case InvalidArgument.desc    => Some(InvalidArgument)
    case DeadlineExceeded.desc   => Some(DeadlineExceeded)
    case NotFound.desc           => Some(NotFound)
    case BadRoute.desc           => Some(BadRoute)
    case AlreadyExists.desc      => Some(AlreadyExists)
    case PermissionDenied.desc   => Some(PermissionDenied)
    case Unauthenticated.desc    => Some(Unauthenticated)
    case ResourceExhausted.desc  => Some(ResourceExhausted)
    case FailedPrecondition.desc => Some(FailedPrecondition)
    case Aborted.desc            => Some(Aborted)
    case OutOfRange.desc         => Some(OutOfRange)
    case Unimplemented.desc      => Some(Unimplemented)
    case Internal.desc           => Some(Internal)
    case Unavailable.desc        => Some(Unavailable)
    case Dataloss.desc           => Some(Dataloss)
    case _                       => None
  }
}

case class TwinagleException(
    code: ErrorCode,
    msg: String,
    meta: Map[String, String] = Map.empty,
    cause: Throwable = null
) extends RuntimeException(msg, cause) {
  def this(cause: Throwable) =
    this(ErrorCode.Internal, cause.toString, Map.empty, cause)
}
