package  ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask._
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime

case class MatchingWork(
  studentId: UUID,
  taskId: UUID,
  sectionId: UUID,
  revision: Long,
  answer: IndexedSeq[Match],
  isComplete: Boolean = false,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Work

object MatchingWork {

  /**
   * Build a long-answer work item from a database result row.
   * @param row
   * @return
   */
  def apply(row: RowData): MatchingWork = {
    MatchingWork(
      studentId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      sectionId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      revision  = row("revision").asInstanceOf[Long],
      answer    = row("answer").asInstanceOf[IndexedSeq[IndexedSeq[Int]]].map { element =>
        Match(element(0), element(1))
      },
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

}

