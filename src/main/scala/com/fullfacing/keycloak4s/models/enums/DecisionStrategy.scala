package com.fullfacing.keycloak4s.models.enums
import enumeratum._

import scala.collection.immutable.IndexedSeq

sealed trait DecisionStrategy extends EnumEntry

object DecisionStrategy extends Enum[DecisionStrategy] {
  override def values: IndexedSeq[DecisionStrategy] = findValues

  case object AFFIRMATIVE extends DecisionStrategy
  case object UNANIMOUS   extends DecisionStrategy
  case object CONSENSUS   extends DecisionStrategy
}