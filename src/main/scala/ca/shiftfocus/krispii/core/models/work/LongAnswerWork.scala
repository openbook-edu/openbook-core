package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime

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
) extends DocumentWork {

  override def toString: String ={
    '"' + response + '"'
  }

}

object LongAnswerWork {


}
