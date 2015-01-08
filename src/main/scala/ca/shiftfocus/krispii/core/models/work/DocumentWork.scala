package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID

trait DocumentWork extends Work {
  val documentId: UUID
}