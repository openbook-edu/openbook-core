package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.lib.UUID

/**
 * The "work" trait is the supertype for work that students have done.
 */
trait Work {
  val sectionId: UUID
  val taskId: UUID
  val studentId: UUID
}