package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import ca.shiftfocus.krispii.core.models.Gfile
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.tasks.Task
import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json._

sealed trait Work extends Evaluation {
  val taskId: UUID
  val studentId: UUID
  val workType: Int
  val isComplete: Boolean
  val gFiles: IndexedSeq[Gfile]
  override def responseToString: String = {
    "work"
  }
}

object Work {
  implicit val jsonWrites = new Writes[Work] {
    def writes(work: Work): JsValue = {
      val jsVal = Json.obj(
        "id" -> work.id,
        "studentId" -> work.studentId,
        "taskId" -> work.taskId,
        "version" -> work.version,
        "workType" -> work.workType,
        "response" -> {
          work match {
            case specific: DocumentWork if specific.response.isDefined => Json.toJson(specific.response.get)
            case specific: QuestionWork => Answers.writes.writes(specific.response)
            case specific: MediaWork => MediaAnswer.writes.writes(specific.fileData)
          }
        },
        "isComplete" -> work.isComplete,
        "grade" -> work.grade,
        "gFiles" -> work.gFiles,
        "createdAt" -> work.createdAt,
        "updatedAt" -> work.updatedAt
      )

      work match {
        case docWork: DocumentWork => jsVal + ("documentId" -> Json.toJson(docWork.documentId))
        case _ => jsVal
      }
    }
  }
}

final case class DocumentWork(
    id: UUID = UUID.randomUUID,
    studentId: UUID,
    taskId: UUID,
    documentId: UUID,
    version: Long = 1L,
    workType: Int = Task.Document,
    response: Option[Document] = None,
    isComplete: Boolean = false,
    grade: String,
    gFiles: IndexedSeq[Gfile] = IndexedSeq.empty[Gfile],
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Work {
  override def responseToString: String = {
    response match {
      case Some(document) => document.plaintext
      case None => ""
    }
  }
}

final case class QuestionWork(
    id: UUID = UUID.randomUUID,
    studentId: UUID,
    taskId: UUID,
    version: Long = 1L,
    workType: Int = Task.Question,
    response: Answers = Answers(),
    isComplete: Boolean = false,
    grade: String,
    gFiles: IndexedSeq[Gfile] = IndexedSeq.empty[Gfile],
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Work {
  override def responseToString: String = {
    response.toString
  }
}

final case class MediaWork(
    id: UUID = UUID.randomUUID,
    studentId: UUID,
    taskId: UUID,
    version: Long = 1L,
    workType: Int = Task.Media,
    fileData: MediaAnswer = MediaAnswer(),
    isComplete: Boolean = false,
    grade: String,
    gFiles: IndexedSeq[Gfile] = IndexedSeq.empty[Gfile],
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Work {
  override def responseToString: String = {
    fileData.toString
  }
}

