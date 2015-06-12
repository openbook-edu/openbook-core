package ca.shiftfocus.krispii.core.models.tasks.questions

import java.util.UUID

import ca.shiftfocus.krispii.core.models.tasks.CommonTaskSettings
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Question {
  val title: String
  val description: String
}
object Question {
  val ShortAnswer = 1
  val Blanks = 2
  val MultipleChoice = 3
  val Ordering = 4
  val Matching = 5

  implicit val reads = new Reads[Question] {
    def reads(json: JsValue) = {
      (json \ "type").asOpt[Int] match {
        case Some(t) if t == ShortAnswer => ShortAnswerQuestion.reads.reads(json)
        case Some(t) if t == Blanks => BlanksQuestion.reads.reads(json)
        case Some(t) if t == MultipleChoice => MultipleChoiceQuestion.reads.reads(json)
        case Some(t) if t == Ordering => OrderingQuestion.reads.reads(json)
        case Some(t) if t == Matching => MatchingQuestion.reads.reads(json)
        case _ => JsError("Invalid type value. Must be an integer from 1 to 5.")
      }
    }
  }
}

case class ShortAnswerQuestion(
  title: String,
  description: String,
  maxLength: String
) extends Question

case class BlanksQuestion(
  title: String,
  description: String,
  text: String,
  inputs: IndexedSeq[(Int, Int)]
) extends Question

case class MultipleChoiceQuestion(
  title: String,
  description: String,
  choices: IndexedSeq[String],
  correct: IndexedSeq[Int],
  singleAnswer: Boolean = true
) extends Question

case class OrderingQuestion(
  title: String,
  description: String,
  choices: IndexedSeq[String]
) extends Question

case class MatchingQuestion(
  title: String,
  description: String,
  choices: IndexedSeq[(String, String)]
) extends Question

object ShortAnswerQuestion {
  implicit val reads: Reads[ShortAnswerQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "maxLength").read[String]
  )(ShortAnswerQuestion.apply _)

  implicit val writes: Writes[ShortAnswerQuestion] = (
    (__ \ "title").write[String] and
    (__ \ "description").write[String] and
    (__ \ "maxLength").write[String]
  )(unlift(ShortAnswerQuestion.unapply))
}

object BlanksQuestion {
  implicit val reads: Reads[BlanksQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "text").read[String] and
    (__ \ "inputs").read[IndexedSeq[(Int, Int)]]
  )(BlanksQuestion.apply _)

  implicit val writes: Writes[BlanksQuestion] = (
    (__ \ "title").write[String] and
    (__ \ "description").write[String] and
    (__ \ "text").write[String] and
    (__ \ "inputs").write[IndexedSeq[(Int, Int)]]
  )(unlift(BlanksQuestion.unapply))
}

object MultipleChoiceQuestion {
  implicit val reads: Reads[MultipleChoiceQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[String]] and
    (__ \ "correct").read[IndexedSeq[Int]] and
    (__ \ "singleAnswer").read[Boolean]
  )(MultipleChoiceQuestion.apply _)

  implicit val writes: Writes[MultipleChoiceQuestion] = (
    (__ \ "title").write[String] and
    (__ \ "description").write[String] and
    (__ \ "choices").write[IndexedSeq[String]] and
    (__ \ "correct").write[IndexedSeq[Int]] and
    (__ \ "singleAnswer").write[Boolean]
  )(unlift(MultipleChoiceQuestion.unapply))
}

object OrderingQuestion {
  implicit val reads: Reads[OrderingQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[String]]
  )(OrderingQuestion.apply _)

  implicit val writes: Writes[OrderingQuestion] = (
    (__ \ "title").write[String] and
    (__ \ "description").write[String] and
    (__ \ "choices").write[IndexedSeq[String]]
  )(unlift(OrderingQuestion.unapply))
}

object MatchingQuestion {
  implicit val reads: Reads[MatchingQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[(String, String)]]
  )(MatchingQuestion.apply _)

  implicit val writes: Writes[MatchingQuestion] = (
    (__ \ "title").write[String] and
    (__ \ "description").write[String] and
    (__ \ "choices").write[IndexedSeq[(String, String)]]
  )(unlift(MatchingQuestion.unapply))
}
