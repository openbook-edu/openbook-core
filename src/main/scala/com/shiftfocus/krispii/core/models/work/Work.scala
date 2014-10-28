package com.shiftfocus.krispii.core.models.work

import com.shiftfocus.krispii.core.lib.UUID

/**
 * The "work" trait is the supertype for work that students have done.
 */
trait Work {
  val sectionId: UUID
  val taskId: UUID
  val studentId: UUID
}