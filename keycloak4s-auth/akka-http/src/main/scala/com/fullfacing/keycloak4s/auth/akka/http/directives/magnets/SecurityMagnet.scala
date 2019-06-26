package com.fullfacing.keycloak4s.auth.akka.http.directives.magnets

import java.util.UUID

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.{extractMethod, extractUnmatchedPath, pass}
import com.fullfacing.keycloak4s.auth.akka.http.Logging
import com.fullfacing.keycloak4s.auth.akka.http.authorisation.Authorisation
import com.fullfacing.keycloak4s.auth.akka.http.authorisation.Authorisation._
import com.fullfacing.keycloak4s.auth.akka.http.directives.Directives._
import com.fullfacing.keycloak4s.auth.akka.http.validation.TokenValidator

trait SecurityMagnet {
  def apply(): Directive0
}

object SecurityMagnet {

  implicit def run(parameters: (Authorisation, UUID))(implicit tokenValidator: TokenValidator): SecurityMagnet = { () =>
    val (securityConfig, cId) = parameters
    authorise(securityConfig, cId)(tokenValidator)
  }

  implicit def run(securityConfig: Authorisation)(implicit tokenValidator: TokenValidator): SecurityMagnet = { () =>
    authorise(securityConfig, UUID.randomUUID())
  }

  private def authorise(securityConfig: Authorisation, correlationId: => UUID)(implicit tokenValidator: TokenValidator): Directive0 = {
    validateToken(correlationId).tflatMap { case (cId, authPayload) =>
      if (securityConfig.policyDisabled()) {
        pass
      } else {
        authoriseResourceServerAccess(authPayload, securityConfig.service)(cId).flatMap { userRoles =>
          extractUnmatchedPath.flatMap { path =>
            extractMethod.flatMap { method =>
              Logging.requestAuthorising(cId)
              val isAuthorised = securityConfig.authoriseRequest(path, method, userRoles)(cId)

              if (isAuthorised) {
                Logging.requestAuthorised(cId)
                pass
              } else {
                authorisationFailed()(cId)
              }
            }
          }
        }
      }
    }
  }
}