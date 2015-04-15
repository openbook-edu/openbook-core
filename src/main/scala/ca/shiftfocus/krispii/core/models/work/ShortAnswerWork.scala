package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.models.document.{Revision, Document}
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime

case class ShortAnswerWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val documentId: UUID,
  override val version: Long = 1L,
  override val response: Option[Document] = None,
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends DocumentWork {

  // TODO - remove
//  override def toString: String ={
//    '"' + {response match {
//      case Some(document) => document.plaintext
//      case None => ""
//    }} + '"'
//  }

}

object ShortAnswerWork {

}
