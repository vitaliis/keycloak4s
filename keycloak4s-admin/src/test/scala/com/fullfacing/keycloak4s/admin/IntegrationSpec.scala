package com.fullfacing.keycloak4s.admin

import cats.effect.{ContextShift, IO}
import com.fullfacing.keycloak4s.admin.client.{Keycloak, KeycloakClient}
import com.fullfacing.keycloak4s.admin.services.{Clients, Groups, IdentityProviders, Roles, Users}
import com.fullfacing.keycloak4s.core.models.{KeycloakConfig, KeycloakError}
import com.softwaremill.sttp.SttpBackend
import org.scalatest._

import scala.concurrent.ExecutionContext.global

class IntegrationSpec extends AsyncFlatSpec with Matchers with Inspectors {
  val clientSecret: String = "ed66f43c-2434-4380-b067-ffb4253eea03" //ServerInitializer.clientSecret.unsafeRunSync()

  /* Keycloak Server Configuration **/
  val authConfig = KeycloakConfig.Auth("master", "admin-cli", clientSecret)
  val keycloakConfig = KeycloakConfig("http", "127.0.0.1", 8080, "master", authConfig)

  /* Keycloak Client Implicits **/
  implicit val context: ContextShift[IO] = IO.contextShift(global)
  implicit val backend: SttpBackend[IO, Nothing] = ServerInitializer.backend
  implicit val client: KeycloakClient[IO, Nothing] = new KeycloakClient[IO, Nothing](keycloakConfig)

  /* Keycloak Services **/
  val userService: Users[IO, Nothing] = Keycloak.Users[IO, Nothing]
  val clientService: Clients[IO, Nothing] = Keycloak.Clients[IO, Nothing]
  val groupService: Groups[IO, Nothing] = Keycloak.Groups[IO, Nothing]
  val roleService: Roles[IO, Nothing] = Keycloak.Roles[IO, Nothing]
  val identityProviderService: IdentityProviders[IO, Nothing] = Keycloak.IdentityProviders[IO, Nothing]

  /* Sub-Services **/
  val realmRoleService: roleService.RealmLevel.type = roleService.RealmLevel
  val clientRoleService: roleService.ClientLevel.type = roleService.ClientLevel

  /* Test Helper Functions **/
  def isSuccessful[A](response: Either[KeycloakError, A]): Assertion =
    response shouldBe a [scala.util.Right[_, _]]
}