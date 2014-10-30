package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime

/**
 * The supertype for tasks. A task is identified by its position
 * within a part, which is in turn identifed by its position within
 * a project.
 *
 */
trait Task {
  val id: UUID
  val partId: UUID
  val position: Int
  val version: Long
  val settings: TaskSettings
  val createdAt: Option[DateTime]
  val updatedAt: Option[DateTime]
}
