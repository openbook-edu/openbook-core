package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.Logger

case class LongAnswerWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val documentId: UUID,
  override val version: Long = 1L,
  override val response: String = "",
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends DocumentWork

object LongAnswerWork {


}
