package com.github.agourlay.cornichon.core

import pureconfig.generic.ProductHint
import pureconfig.{ CamelCase, ConfigFieldMapping }

import scala.concurrent.duration._

case class Config(
    executeScenariosInParallel: Boolean = true,
    requestTimeout: FiniteDuration = 2000.millis,
    globalBaseUrl: String = "",
    traceRequests: Boolean = false,
    warnOnDuplicateHeaders: Boolean = false,
    failOnDuplicateHeaders: Boolean = false,
    addAcceptGzipByDefault: Boolean = true,
    disableCertificateVerification: Boolean = false,
    followRedirect: Boolean = false)

object Config {
  implicit val hint = ProductHint[Config](allowUnknownKeys = false, fieldMapping = ConfigFieldMapping(CamelCase, CamelCase))
}