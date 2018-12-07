package com.soundcloud.twirp.error

import play.api.libs.json.{JsString, Json, Writes}

sealed abstract class ErrorCode(val name: String, val httpCode: Int)

case object Canceled extends ErrorCode("canceled", 408) // RequestTimeout
case object Unknown extends ErrorCode("unknown", 500) // Internal Server Error
case object InvalidArgument extends ErrorCode("invalid_argument", 400) // BadRequest
case object DeadlineExceeded extends ErrorCode("deadline_exceeded", 408) // RequestTimeout
case object NotFound extends ErrorCode("not_found", 404) // Not Found
case object BadRoute extends ErrorCode("bad_route", 404) // Not Found
case object AlreadyExists extends ErrorCode("already_exists", 409) // Conflict
case object PermissionDenied extends ErrorCode("permission_denied", 403) // Forbidden
case object Unauthenticated extends ErrorCode("unauthenticated", 401) // Unauthorized
case object ResourceExhausted extends ErrorCode("resource_exhausted", 403) // Forbidden
case object FailedPrecondition extends ErrorCode("failed_precondition", 412) // Precondition Failed
case object Aborted extends ErrorCode("aborted", 409) // Conflict
case object OutOfRange extends ErrorCode("out_of_range", 400) // Bad Request
case object Unimplemented extends ErrorCode("unimplemented", 501) // Not Implemented
case object Internal extends ErrorCode("internal", 500) // Internal Server Error
case object Unavailable extends ErrorCode("unavailable", 503) // Service Unavailable
case object DataLoss extends ErrorCode("dataloss", 500) // Internal Server Error

case class Error(code: ErrorCode, msg: String, meta: Option[Map[String, String]]) {
  def error(): String = s"twirp error ${code.name}: $msg"
}

object Error {

  implicit val errorCodeWrites: Writes[ErrorCode] = Writes[ErrorCode](ec => JsString(ec.name))
  implicit val errorWrites: Writes[Error] = Json.writes[Error]


  private val nameToErrorCode: Map[String, ErrorCode] = Map(
    Canceled.name -> Canceled,
    Unknown.name -> Unknown,
    InvalidArgument.name -> InvalidArgument,
    DeadlineExceeded.name -> DeadlineExceeded,
    NotFound.name -> NotFound,
    BadRoute.name -> BadRoute,
    AlreadyExists.name -> AlreadyExists,
    PermissionDenied.name -> PermissionDenied,
    Unauthenticated.name -> Unauthenticated,
    ResourceExhausted.name -> ResourceExhausted,
    FailedPrecondition.name -> FailedPrecondition,
    Aborted.name -> Aborted,
    OutOfRange.name -> OutOfRange,
    Unimplemented.name -> Unimplemented,
    Internal.name -> Internal,
    Unavailable.name -> Unavailable,
    DataLoss.name -> DataLoss,
  )
  private val httpCodeToErrorCode: Map[Int, ErrorCode] = Map(
    Canceled.httpCode -> Canceled,
    Unknown.httpCode -> Unknown,
    InvalidArgument.httpCode -> InvalidArgument,
    DeadlineExceeded.httpCode -> DeadlineExceeded,
    NotFound.httpCode -> NotFound,
    BadRoute.httpCode -> BadRoute,
    AlreadyExists.httpCode -> AlreadyExists,
    PermissionDenied.httpCode -> PermissionDenied,
    Unauthenticated.httpCode -> Unauthenticated,
    ResourceExhausted.httpCode -> ResourceExhausted,
    FailedPrecondition.httpCode -> FailedPrecondition,
    Aborted.httpCode -> Aborted,
    OutOfRange.httpCode -> OutOfRange,
    Unimplemented.httpCode -> Unimplemented,
    Internal.httpCode -> Internal,
    Unavailable.httpCode -> Unavailable,
    DataLoss.httpCode -> DataLoss,
  )

  def from(code: String): ErrorCode = {
    nameToErrorCode.get(code) match {
      case Some(ec) => ec
      case None => Internal
    }
  }
  def from(httpCode: Int): ErrorCode = {
    httpCodeToErrorCode.get(httpCode) match {
      case Some(ec) => ec
      case None => Internal
    }
  }
}





