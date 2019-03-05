package com.fullfacing.keycloak4s.models

case class EventRepresentation(clientId: Option[String],
                               details: Option[Map[_, _]],
                               error: Option[String],
                               ipAddress: Option[String],
                               realmId: Option[String],
                               time: Option[Long],
                               `type`: Option[String],
                               userId: Option[String])