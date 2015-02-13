package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class ShortAnswerWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  courseId: UUID,
  override val documentId: UUID,
  override val version: Long = 0,
  override val answer: String = "",
  isComplete: Boolean = false,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends DocumentWork

object ShortAnswerWork {

  /**
   * Build a long-answer work item from a database result row.
   * @param row
   * @return
   */
  def apply(row: RowData): ShortAnswerWork = {
    ShortAnswerWork(
      studentId = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      courseId = UUID(row("course_id").asInstanceOf[Array[Byte]]),
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      version  = row("version").asInstanceOf[Long],
      answer    = "",
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

}
