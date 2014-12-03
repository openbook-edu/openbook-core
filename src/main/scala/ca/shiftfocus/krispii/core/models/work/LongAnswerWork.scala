package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class LongAnswerWork(
  studentId: UUID,
  taskId: UUID,
  sectionId: UUID,
  revision: Long,
  override val version: Long = 0,
  answer: String,
  isComplete: Boolean = false,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Work

object LongAnswerWork {

  /**
   * Build a long-answer work item from a database result row.
   * @param row
   * @return
   */
  def apply(row: RowData): LongAnswerWork = {
    LongAnswerWork(
      studentId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      sectionId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      revision  = row("revision").asInstanceOf[Long],
      version   = row("version").asInstanceOf[Long],
      answer    = row("answer").asInstanceOf[String],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

}