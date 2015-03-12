package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class ShortAnswerWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val documentId: UUID,
  override val version: Long = 1L,
  override val answer: String = "",
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends DocumentWork

object ShortAnswerWork {

}
