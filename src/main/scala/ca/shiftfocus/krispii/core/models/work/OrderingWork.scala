package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class OrderingWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val version: Long,
  override val answer: IndexedSeq[Int],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Work

object OrderingWork {


}
