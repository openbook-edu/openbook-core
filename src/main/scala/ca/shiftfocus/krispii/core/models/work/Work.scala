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
            case specific: QuestionWork => Json.toJson(specific.response)
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
    response: Map[Int, Answer] = Map(),
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

//case class Answers(map: Map[Int, Answer]) {
//  implicit val reads = new Reads[Answers] {
//    def reads(json: JsValue) = {
//      json.
//    }
//  }
//}

trait Answer {
  val questionType: Int
  def answers(question: Question): Boolean = question.questionType == questionType
}
object Answer {
  implicit val reads = new Reads[Answer] {
    def reads(json: JsValue) = {
      (json \ "answerType").asOpt[Int] match {
        case Some(t) if t == Question.ShortAnswer => ShortAnswerAnswer.reads.reads(json)
        case Some(t) if t == Question.Blanks => BlanksAnswer.reads.reads(json)
        case Some(t) if t == Question.MultipleChoice => MultipleChoiceAnswer.reads.reads(json)
        case Some(t) if t == Question.Ordering => OrderingAnswer.reads.reads(json)
        case Some(t) if t == Question.Matching => MatchingAnswer.reads.reads(json)
        case _ => JsError("Invalid answer type")
      }
    }
  }
  implicit val writes = new Writes[Answer] {
    def writes(answer: Answer) = {
      answer match {
        case shortAnswer: ShortAnswerAnswer => ShortAnswerAnswer.writes.writes(shortAnswer)
        case blanks: BlanksAnswer => BlanksAnswer.writes.writes(blanks)
        case multipleChoice: MultipleChoiceAnswer => MultipleChoiceAnswer.writes.writes(multipleChoice)
        case ordering: OrderingAnswer => OrderingAnswer.writes.writes(ordering)
        case matching: MatchingAnswer => MatchingAnswer.writes.writes(matching)
      }
    }
  }
}

final case class ShortAnswerAnswer(answer: String) extends Answer { override val questionType = Question.ShortAnswer }
final case class BlanksAnswer(answer: IndexedSeq[String]) extends Answer { override val questionType = Question.Blanks }
final case class MultipleChoiceAnswer(answer: IndexedSeq[Int]) extends Answer { override val questionType = Question.MultipleChoice }
final case class OrderingAnswer(answer: IndexedSeq[Int]) extends Answer { override val questionType = Question.Ordering }
final case class MatchingAnswer(answer: IndexedSeq[Match]) extends Answer { override val questionType = Question.Matching }

object ShortAnswerAnswer {
  implicit val reads = new Reads[ShortAnswerAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[String] match {
      case Some(answer) => JsSuccess(ShortAnswerAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
  implicit val writes = new Writes[ShortAnswerAnswer] {
    def writes(answer: ShortAnswerAnswer) = Json.obj(
      "answerTye"
    )
  }
}

object BlanksAnswer {
  implicit val reads = new Reads[BlanksAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[String]] match {
      case Some(answer) => JsSuccess(BlanksAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
}

object MultipleChoiceAnswer {
  implicit val reads = new Reads[MultipleChoiceAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Int]] match {
      case Some(answer) => JsSuccess(MultipleChoiceAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
}

object OrderingAnswer {
  implicit val reads = new Reads[OrderingAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Int]] match {
      case Some(answer) => JsSuccess(OrderingAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
}

object MatchingAnswer {
  implicit val reads = new Reads[MatchingAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Match]] match {
      case Some(answer) => JsSuccess(MatchingAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
}
