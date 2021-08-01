package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import ca.shiftfocus.krispii.core.models.tasks.questions.Question
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json, Reads, Writes, __}
import play.api.libs.functional.syntax._

case class Answers(underlying: Map[UUID, Answer] = Map()) {
  def updated(key: UUID, value: Answer): Answers = Answers(underlying.updated(key, value))
}

/*

  {
    "idA": {"questionType": 2, "answer": "hello},
    "idB": {...}
  }

 */

object Answers {
  implicit val reads = new Reads[Answers] {
    def reads(json: JsValue) = {
      json.validate[JsObject] match {
        case jsSuccess: JsSuccess[JsObject] => {
          val jsObj = jsSuccess.get
          val answers = jsObj.value.map {
            case (key, value) => {
              val id = UUID.fromString(key)
              val answerResult = Answer.reads.reads(value)

              if (answerResult.isError) {
                answerResult.asInstanceOf[JsError]
              }
              else {
                JsSuccess((id, answerResult.get))
              }
            }
          }

          if (answers.exists({ item => item.isError })) {
            val errors = answers.filter({ item => item.isError }).mkString("; ")
            // return error
            JsError(errors)
          }
          else {
            JsSuccess(Answers(answers.map(_.get).toMap))
          }
        }
        case error: JsError => JsError("JSON had invalid format.")
      }
    }
  }
  implicit val writes = new Writes[Answers] {
    def writes(answers: Answers) = {
      JsObject(answers.underlying.map {
        case ((key, answer)) =>
          (key.toString, Answer.writes.writes(answer))
      })
    }
  }
}

trait Answer {
  val questionType: Int
  def answers(question: Question): Boolean = question.questionType == questionType
}
object Answer {
  implicit val reads: Reads[Answer] = new Reads[Answer] {
    def reads(json: JsValue) = {
      (json \ "questionType").asOpt[Int] match {
        case Some(t) if t == Question.ShortAnswer => ShortAnswer.reads.reads(json)
        case Some(t) if t == Question.Blanks => BlanksAnswer.reads.reads(json)
        case Some(t) if t == Question.MultipleChoice => MultipleChoiceAnswer.reads.reads(json)
        case Some(t) if t == Question.Ordering => OrderingAnswer.reads.reads(json)
        case Some(t) if t == Question.Matching => MatchingAnswer.reads.reads(json)
        case _ => JsError("Invalid answer type")
      }
    }
  }
  implicit val writes: Writes[Answer] = new Writes[Answer] {
    def writes(answer: Answer) = {
      answer match {
        case shortAnswer: ShortAnswer => ShortAnswer.writes.writes(shortAnswer)
        case blanks: BlanksAnswer => BlanksAnswer.writes.writes(blanks)
        case multipleChoice: MultipleChoiceAnswer => MultipleChoiceAnswer.writes.writes(multipleChoice)
        case ordering: OrderingAnswer => OrderingAnswer.writes.writes(ordering)
        case matching: MatchingAnswer => MatchingAnswer.writes.writes(matching)
      }
    }
  }
}

final case class ShortAnswer(answer: String) extends Answer { override val questionType = Question.ShortAnswer }
final case class BlanksAnswer(answer: IndexedSeq[String]) extends Answer { override val questionType = Question.Blanks }
final case class MultipleChoiceAnswer(answer: IndexedSeq[Int]) extends Answer { override val questionType = Question.MultipleChoice }
final case class OrderingAnswer(answer: IndexedSeq[Int]) extends Answer { override val questionType = Question.Ordering }
final case class MatchingAnswer(answer: IndexedSeq[MatchingAnswer.Match]) extends Answer { override val questionType = Question.Matching }

object ShortAnswer {
  implicit val reads = new Reads[ShortAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[String] match {
      case Some(answer) => JsSuccess(ShortAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
  implicit val writes = new Writes[ShortAnswer] {
    def writes(shortAnswer: ShortAnswer) = Json.obj(
      "questionType" -> shortAnswer.questionType,
      "answer" -> shortAnswer.answer
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
  implicit val writes = new Writes[BlanksAnswer] {
    def writes(blanksAnswer: BlanksAnswer) = Json.obj(
      "questionType" -> blanksAnswer.questionType,
      "answer" -> blanksAnswer.answer
    )
  }
}

object MultipleChoiceAnswer {
  implicit val reads = new Reads[MultipleChoiceAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Int]] match {
      case Some(answer) => JsSuccess(MultipleChoiceAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
  implicit val writes = new Writes[MultipleChoiceAnswer] {
    def writes(multipleChoiceAnswer: MultipleChoiceAnswer) = Json.obj(
      "questionType" -> multipleChoiceAnswer.questionType,
      "answer" -> multipleChoiceAnswer.answer
    )
  }
}

object OrderingAnswer {
  implicit val reads = new Reads[OrderingAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Int]] match {
      case Some(answer) => JsSuccess(OrderingAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
  implicit val writes = new Writes[OrderingAnswer] {
    def writes(orderingAnswer: OrderingAnswer) = Json.obj(
      "questionType" -> orderingAnswer.questionType,
      "answer" -> orderingAnswer.answer
    )
  }
}

object MatchingAnswer {
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

  implicit val reads = new Reads[MatchingAnswer] {
    def reads(json: JsValue) = (json \ "answer").asOpt[IndexedSeq[Match]] match {
      case Some(answer) => JsSuccess(MatchingAnswer(answer))
      case None => JsError("'answer' parameter not given")
    }
  }
  implicit val writes = new Writes[MatchingAnswer] {
    def writes(matchingAnswer: MatchingAnswer) = Json.obj(
      "questionType" -> matchingAnswer.questionType,
      "answer" -> matchingAnswer.answer
    )
  }
}

case class MediaAnswer(
  mediaType: Option[String] = None,
  fileName: Option[String] = None,
  serverFileName: Option[String] = None,
  // We store the size of a file in Bytes
  size: Option[Long] = None,
  isPublic: Option[Boolean] = None
)
object MediaAnswer {
  implicit val reads = new Reads[MediaAnswer] {
    def reads(json: JsValue) = {
      JsSuccess(
        MediaAnswer(
          (json \ "mediaType").asOpt[String],
          (json \ "fileName").asOpt[String],
          (json \ "serverFileName").asOpt[String],
          (json \ "size").asOpt[Long],
          (json \ "isPublic").asOpt[Boolean]
        )
      )
    }
  }
  implicit val writes = new Writes[MediaAnswer] {
    def writes(mediaAnswer: MediaAnswer): JsValue = {
      Json.obj(
        "mediaType" -> mediaAnswer.mediaType,
        "fileName" -> mediaAnswer.fileName,
        "fileName" -> mediaAnswer.fileName,
        "serverFileName" -> mediaAnswer.serverFileName,
        "size" -> mediaAnswer.size,
        "isPublic" -> mediaAnswer.isPublic
      )
    }
  }
}
