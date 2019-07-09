# keycloak4s
[![CircleCI](https://circleci.com/gh/fullfacing/keycloak4s.svg?style=shield&circle-token=0788f14be0abb7f8ab8194fbd2cd179122b3ee85)](https://circleci.com/gh/fullfacing/keycloak4s)
[![codecov](https://codecov.io/gh/fullfacing/keycloak4s/branch/master/graph/badge.svg?token=WKbJaagGhz)](https://codecov.io/gh/fullfacing/keycloak4s)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.fullfacing/keycloak4s-core_2.12.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/fullfacing/keycloak4s-core_2.12/)

A Scala-based middleware API for [Keycloak](https://www.keycloak.org/).

keycloak4s is an opinionated Scala built API that serves as a bridge between any Scala project and a Keycloak server, allowing access to the server's [Admin API](https://www.keycloak.org/docs-api/6.0/rest-api/index.html) as well as providing adapters to validate Keycloak's access tokens and authorize requests via a JSON config file inspired by their [policy enforcement configuration](https://www.keycloak.org/docs/latest/authorization_services/index.html#_enforcer_filter).

The project is split into the following modules, each as a separate dependency:
* `keycloak4s-core`: Contains core functionality and shared models across other modules.
* `keycloak4s-admin`: Allows access to Keycloak's [Admin API][Admin-API]. Utilizes SoftwareMill's [sttp](https://github.com/softwaremill/sttp) and Cats Effect' [Concurrent](https://typelevel.org/cats-effect/typeclasses/concurrent.html) to allow for customization of the HTTP client and concurrency monad to be used, respectively.
* `keycloak4s-admin-monix`: A more concrete implementation of keycloak-admin using [Monix][Monix]. Contains additional reactive streaming functionality.
* `keycloak4s-akka-http`: A client adapter capable of validating Keycloak's access tokens and providing authorization for [Akka-HTTP][Akka-Http] requests.

### Contents
1. [Installation](#Installation)
2. [Module: keycloak4s-admin](#keycloak4s-admin)
3. [Module: keycloak4s-admin-monix](#keycloak4s-admin-monix)
4. [Module: keycloak4s-akka-http](#keycloak4s-akka-http)
5. [Logging and Error Handling](#LoggingAndErrorHandling)

## Installation

Each module can be pulled into a project separately via the following SBT dependencies:
* keycloak4s-core:        `"com.fullfacing" %% "keycloak4s-core" % "1.0"`
* keycloak4s-admin:       `"com.fullfacing" %% "keycloak4s-admin" % "1.0"`
* keycloak4s-admin-monix: `"com.fullfacing" %% "keycloak4s-monix" % "1.0"`
* keycloak4s-akka-http:   `"com.fullfacing" %% "keycloak4s-akka-http" % "1.0"`

(The core module is already included in all other modules and is not required to be pulled in under normal circumstances.)

## Module: keycloak4s-admin <a name="keycloak4s-admin"></a>
In order to make calls to Keycloak's [Admin API][Admin-API] a client needs to be created with the correct server details and credentials to connect to the Keycloak server, followed by a service handler that can invoke the calls. The process can be broken down into the following steps:

**1 - Create a KeycloakConfig**<br/>
The KeycloakConfig contains the server details (URL scheme, host and port) to reach the Keycloak server, the credentials (realm name, client ID and client secret) for a [service account](https://www.keycloak.org/docs/latest/server_admin/index.html#_service_accounts) enabled admin client, and the name of the realm that will be accessed.

*Example:*
```scala
import com.fullfacing.keycloak4s.core.models.KeycloakConfig

val authConfig = KeycloakConfig.Auth(
    realm         = "master",
    clientId      = "admin-cli",
    clientSecret  = "b753f3ba-c4e7-4f3f-ac16-a074d4d89353"
)
   
val keycloakConfig = KeycloakConfig(
    scheme  = "http",
    host    = "fullfacing.com/keycloak",
    port    = 8080,
    realm   = "demo",
    authn   = authConfig
)
```

**2 - Create a KeycloakClient**<br/>
The KeycloakClient handles the HTTP calls to the KeycloakServer, it requires a KeycloakConfig and a sttp backend (the types given to the KeycloakClient must match the sttp backend's types). Alternatively the Akka/Monix alternative module can be used for a concrete implementation, see the keycloak4s-admin-monix segment for more details.

*Example:*
```scala
import com.fullfacing.keycloak4s.admin.client.KeycloakClient
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.AsyncHttpClientBackend
import monix.eval.Task
import monix.reactive.Observable

implicit val backend: SttpBackend[Task, Observable[ByteBuffer]] = AsyncHttpClientBackend()
implicit val keycloakClient: KeycloakClient[Task, Observable[ByteBuffer]] = new KeycloakClient[Task, Observable[ByteBuffer]](config)
```

**3 - Create a service handler**<br/>
A service handler in this context contains the admin calls relevant to a specific aspect of Keycloak. For example the Users service contains calls that creates, retrieves and manipulates Keycloak Users. It requires an implicit KeycloakClient in scope.

*Example:*
```scala
import com.fullfacing.keycloak4s.admin.client.Keycloak

val usersService = Keycloak.Users[Task, Observable[ByteBuffer, Any]]
val rolesService = Keycloak.Roles[Task, Observable[ByteBuffer, Any]]
```

**4 - Invoke the calls**<br/> 
The relevant admin API calls can be invoked from a service handler, which will automatically handle calls for access and refresh tokens in the background through the client credentials flow.

*Example:*
```scala
import cats.data.EitherT
import com.fullfacing.keycloak4s.core.models.{Group, User}

val newUser = User.Create(username = "ff_keycloak_user_01", enabled = true)
val newGroup = Group.Create(name = "ff_keycloak_group_01")

for {
  u <- EitherT(usersService.createAndRetrieve(newUser))
  g <- EitherT(groupsService.createAndRetrieve(newGroup))
  _ <- EitherT(usersService.addToGroup(u.id, g.id))
} yield (u, g)
```

The majority of the functions in a service handler directly corresponds to a admin API route, however a few are composite functions created for convenience, such as the `createAndRetrieve` function seen in the above example which chains a create call and a fetch call.

## Module: keycloak4s-admin-monix <a name="keycloak4s-admin-monix"></a>
An alternative of keycloak4s-admin typed to Monix with [Tasks][Task] as the response wrapper and [Observables][Observable] as the streaming type, removing the need to set up the types for KeycloakClient or the service handlers. Additionally this module contains reactive streaming variants of the fetch calls allowing for batch retrieval and processing, and a Monix-wrapped Akka-HTTP based sttp backend ready for use. 

The steps to make calls remains mostly the same as in the keycloak4s-admin, below is an example with the prebuilt sttp backend specifically, refer to the keycloak4s-admin segment for additional information.

*Example (truncated):*
```scala
import com.fullfacing.keycloak4s.admin.monix.backends.AkkaMonixHttpBackend
import com.fullfacing.keycloak4s.admin.monix.client.{Keycloak, KeycloakClient}

implicit val backend: SttpBackend[Task, Observable[ByteString]] = AkkaMonixHttpBackend()
implicit val monixClient: KeycloakClient = new KeycloakClient(...) 

val usersService = Keycloak.Users

usersService.fetch()
```

To use the streaming variants of this module simply replace any `fetch` call with `fetchS`, which takes an additional `batchSize` parameter. The function returns an [Observable][Observable] and operates in batches by calling to retrieve the specified batch size, processing the batched results, then making the call for the next batch.

*Example:*
```scala
val usersService = Keycloak.Users

usersService.fetchS(batchSize = 20)
        .dropWhile(_.enabled)
        .map(_.id)
        .toListL
```

A `fetchL` variant is also available which performs the same batch streaming, but automatically converts the Observable to a List of Task when the stream has completed.

## Module: keycloak4s-akka-http <a name="keycloak4s-akka-http"></a>

TODO

## Logging and Error Handling <a name="LoggingAndErrorHandling"></a>
keycloak4s has customized logging spanning over the trace, debug and error levels using [SLF4J](https://www.slf4j.org/), the logging output can easily be controlled (for example with [Logback](https://logback.qos.ch/)) using the following Logger names:
* Top level: `keycloak4s`
* keycloak4s-admin module: `keycloak4s.admin`
* keycloak4s-akka-http module: `keycloak4s.auth`

[Monix]: https://monix.io/
[Task]: https://monix.io/docs/3x/eval/task.html
[Observable]: https://monix.io/docs/3x/reactive/observable.html
[Akka-HTTP]: https://doc.akka.io/docs/akka-http/current/introduction.html
[Admin-API]: https://www.keycloak.org/docs-api/5.0/rest-api/index.html

The `KeycloakError` returned by keycloak4s extends `Throwable`, and have the following subtypes:
* `KeycloakThrowable` - Merely wraps a Throwable
* `KeycloakException` - Contains a status code, status text, an error message and optionally finer details.
* `KeycloakSttpException` - Contains the HTTP response details sent back from the sttp client, along with information of the request that was sent.
