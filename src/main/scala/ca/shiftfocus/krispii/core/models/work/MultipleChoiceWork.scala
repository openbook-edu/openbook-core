package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.lib.UUID

case class MultipleChoiceWork(
  sectionId: UUID,
  taskId: UUID,
  studentId: UUID,
  answer: IndexedSeq[Int]
) extends Work