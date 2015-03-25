package  ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask._
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class MatchingWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val version: Long,
  override val response: IndexedSeq[Match],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Work

object MatchingWork {


}
