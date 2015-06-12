package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.models.document.Document
import java.util.UUID
import ca.shiftfocus.krispii.core.models.tasks.questions._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * The "work" trait is the supertype for work that students have done.
 *
 * Sealed so that all possible sub-types must be defined here.
 */
sealed trait Work {
  val id: UUID
  val studentId: UUID
  val taskId: UUID
  val version: Long
  val isComplete: Boolean
  val createdAt: DateTime
  val updatedAt: DateTime
  def responseToString: String = {
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
        "response" -> {
          work match {
            case specific: DocumentWork if specific.response.isDefined => Json.toJson(specific.response.get)
            case specific: QuestionWork if specific.response.isDefined => Json.toJson(specific.response.get)
            case _ => JsNull
          }
        },
        "isComplete" -> work.isComplete,
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
    response: Option[Document] = None,
    isComplete: Boolean = false,
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
    response: Map[Int, Answer[_]] = Map(),
    isComplete: Boolean = false,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Work {
  override def responseToString: String = {
    response match {
      case Some(answers) => answers.toString
      case None => ""
    }
  }
}

case class Match(left: Int, right: Int)
object Match {
  implicit val reads: Reads[Match] = (
    (__ \ "left").read[Int] and
    (__ \ "right").read[Int]
  )(Match.apply _)

  implicit val writes: Writes[Match] = (
    (__ \ "left").write[Int] and
    (__ \ "right").write[Int]
  )(unlift(Match.unapply))
}

//case class Answers(map: Map[Int, Answer[_]]) {
//  implicit val reads = new Reads[Answers] {
//    def reads(json: JsValue) = {
//
//    }
//  }
//}

sealed trait Answer[Q] {
  def answers(question: Question): Boolean = question.isInstanceOf[Q]
}
final case class ShortAnswerAnswer(answer: String) extends Answer[ShortAnswerQuestion]
final case class BlanksAnswer(answer: IndexedSeq[String]) extends Answer[BlanksQuestion]
final case class MultipleChoiceAnswer(answer: IndexedSeq[Int]) extends Answer[MultipleChoiceQuestion]
final case class OrderingAnswer(answer: IndexedSeq[Int]) extends Answer[OrderingQuestion]
final case class MatchingAnswer(answer: IndexedSeq[Match]) extends Answer[MatchingQuestion]
