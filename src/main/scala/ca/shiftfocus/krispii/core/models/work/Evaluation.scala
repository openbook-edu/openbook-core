package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import org.joda.time.DateTime

trait Evaluation {
  val id: UUID
  val version: Long
  val grade: String //needs to accommodate "9.5", "A" etc.
  val createdAt: DateTime
  val updatedAt: DateTime
  def responseToString: String = {
    "evaluation"
  }
}
