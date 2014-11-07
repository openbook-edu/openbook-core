package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.lib.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class MultipleChoiceWork(
  studentId: UUID,
  taskId: UUID,
  sectionId: UUID,
  revision: Long,
  answer: IndexedSeq[Int],
  createdAt: Option[DateTime],
  updatedAt: Option[DateTime]
) extends Work

object MultipleChoiceWork {

  /**
   * Build a long-answer work item from a database result row.
   * @param row
   * @return
   */
  def apply(row: RowData): MultipleChoiceWork = {
    MultipleChoiceWork(
      studentId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      sectionId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      revision  = row("revision").asInstanceOf[Long],
      answer    = row("answer").asInstanceOf[IndexedSeq[Int]],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

}