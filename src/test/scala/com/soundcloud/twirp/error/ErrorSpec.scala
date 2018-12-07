package com.soundcloud.twirp.error

import org.specs2.mutable.Specification
import play.api.libs.json.{JsObject, Json}

class ErrorSpec extends Specification {
  "formats Error to Json correctly wihtout metadata" in {
    val e = Error(Canceled, "or is it cancelled?", None)
    Json.toJson(e) ==== Json.parse("{\n  \"code\": \"canceled\",\n  \"msg\": \"or is it cancelled?\"\n}")
  }
  "formats Error to Json correctly with metadata" in {
    val e = Error(
      PermissionDenied,
      "Thou shall not pass",
      Some(
        Map(
          "target" -> "Balrog", "power" -> "999"
        )
      )
    )
    Json.toJson(e) ==== Json.parse(
      """
        |{
        |  "code": "permission_denied",
        |  "msg": "Thou shall not pass",
        |  "meta": {
        |    "target": "Balrog",
        |    "power": "999"
        |  }
        |}
      """.stripMargin)
  }
}
