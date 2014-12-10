package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class OrderingWork(
  studentId: UUID,
  taskId: UUID,
  classId: UUID,
  revision: Long,
  answer: IndexedSeq[Int],
  isComplete: Boolean = false,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Work

object OrderingWork {

  /**
   * Build a long-answer work item from a database result row.
   * @param row
   * @return
   */
  def apply(row: RowData): OrderingWork = {
    OrderingWork(
      studentId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      classId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      revision  = row("revision").asInstanceOf[Long],
      answer    = row("answer").asInstanceOf[IndexedSeq[Int]],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

}