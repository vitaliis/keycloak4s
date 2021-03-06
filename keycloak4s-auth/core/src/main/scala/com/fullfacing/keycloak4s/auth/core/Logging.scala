package com.fullfacing.keycloak4s.auth.core

import java.util.UUID

import com.fullfacing.keycloak4s.core.Exceptions.ConfigInitialisationException
import com.fullfacing.keycloak4s.core.logging.Logging._
import com.fullfacing.keycloak4s.core.logging._
import com.fullfacing.keycloak4s.core.models.KeycloakException
import com.fullfacing.keycloak4s.core.models.enums.TokenType
import org.slf4j.{Logger, LoggerFactory}

object Logging {

  implicit val logger: Logger = LoggerFactory.getLogger("keycloak4s.auth")

  /* Validation Logging **/

  def jwksRequest(realm: => String, cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Requesting public keys from Keycloak for Realm $gr$realm.$rs")

  def jwksCache(realm: => String, cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Cached public keys for Realm $gr$realm$cy found.$rs")

  def jwksRetrieved(realm: => String, cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Public keys retrieved for Realm $gr$realm.$rs")

  def jwksRequestFailed(cId: UUID, ex: Throwable): Unit =
    logger.error(s"${cIdErr(cId)}", ex)

  def tokenValidating(cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Validating bearer token...$rs")

  def tokenValidated(cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Bearer token(s) successfully validated.$rs")

  def tokenValidationFailed(cId: UUID, ex: Throwable, tokenType: TokenType): Unit =
    logger.error(s"${cIdErr(cId)}${tokenType.value} validation failed.", ex)

  def tokenValidationFailed(cId: UUID, exMessage: String, tokenType: TokenType): Unit =
    logger.error(s"${cIdErr(cId)}${tokenType.value} validation failed - $exMessage")

  /* Authorization Logging **/

  def requestAuthorizing(cId: => UUID, path: => String, method: => String): Unit = {
    logger.logDebugIff(s"${cIdLog(cId)}Authorizing $gr${method}$cy request...$rs")
    logger.logTrace(s"${cIdLog(cId)}Authorizing $gr${method}$cy request for path $gr$path$cy...$rs")
  }

  def requestAuthorized(cId: => UUID): Unit =
    logger.logTrace(s"${cIdLog(cId)}Request successfully authorized.$rs")

  def authorizationDenied(cId: UUID, service: String): Unit =
    logger.logDebug(s"$re${cIdErr(cId)}Authorization denied, user is not allowed access to $service.$rs")

  def authorizationPathDenied(cId: UUID, method: String, path: String): Unit =
    logger.logDebug(s"$re${cIdErr(cId)}Authorization denied, user is not allowed access to ${method} $path.$rs")

  def configFileNotFound(filename: String): Unit =
    logger.error(s"Policy Enforcement configuration JSON file $filename not found.")

  def configSetupError(): Unit =
    logger.error("Failed to parse policy configuration JSON file.")

  def authResourceNotFound(ar: String): Unit =
    logger.error(s"Authorization initialization failed: Segment variable $ar not found in JSON configuration.")

  /* Logging Helpers **/

  def logException(exception: KeycloakException)(log: => Unit): KeycloakException = {
    log; exception
  }

  def authConfigInitException(): Nothing = {
    configSetupError()
    throw new ConfigInitialisationException
  }

  def logValidationEx(exception: KeycloakException, tokenType: TokenType)(implicit cId: UUID): KeycloakException = {
    if (exception.details.isDefined) {
      Logging.tokenValidationFailed(cId, exception, tokenType)
    } else {
      Logging.tokenValidationFailed(cId, exception.message.getOrElse("An unexpected error occurred."), tokenType)
    }

    exception
  }
}
