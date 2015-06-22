package ca.shiftfocus.krispii.core.models.tasks.questions

import java.util.UUID

import ca.shiftfocus.krispii.core.models.tasks.CommonTaskSettings
import ca.shiftfocus.krispii.core.models.tasks.questions.MatchingQuestion.Match
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Question {
  val title: String
  val description: String
  val questionType: Int
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
  implicit val writes = new Writes[Question] {
    def writes(question: Question) = {
      question match {
        case shortAnswer: ShortAnswerQuestion => ShortAnswerQuestion.writes.writes(shortAnswer)
        case blanks: BlanksQuestion => BlanksQuestion.writes.writes(blanks)
        case multipleChoice: MultipleChoiceQuestion => MultipleChoiceQuestion.writes.writes(multipleChoice)
        case ordering: OrderingQuestion => OrderingQuestion.writes.writes(ordering)
        case matching: MatchingQuestion => MatchingQuestion.writes.writes(matching)
      }
    }
  }
}

case class ShortAnswerQuestion(
    title: String,
    description: String,
    maxLength: Int
) extends Question {
  override val questionType = Question.ShortAnswer
}

case class BlanksQuestion(
    title: String,
    description: String,
    text: String,
    inputs: IndexedSeq[BlanksQuestion.Blank]
) extends Question {
  override val questionType = Question.Blanks
}

case class MultipleChoiceQuestion(
    title: String,
    description: String,
    choices: IndexedSeq[String],
    correct: IndexedSeq[Int],
    singleAnswer: Boolean = true
) extends Question {
  override val questionType = Question.MultipleChoice
}

case class OrderingQuestion(
    title: String,
    description: String,
    choices: IndexedSeq[String]
) extends Question {
  override val questionType = Question.Ordering
}

case class MatchingQuestion(
    title: String,
    description: String,
    choices: IndexedSeq[MatchingQuestion.Match]
) extends Question {
  override val questionType = Question.Matching
}

object ShortAnswerQuestion {
  implicit val reads: Reads[ShortAnswerQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "maxLength").read[Int]
  )(ShortAnswerQuestion.apply _)

  implicit val writes = new Writes[ShortAnswerQuestion] {
    def writes(question: ShortAnswerQuestion) = {
      Json.obj(
        "title" -> question.title,
        "description" -> question.description,
        "maxLength" -> question.maxLength,
        "type" -> question.questionType
      )
    }
  }
}

object BlanksQuestion {
  case class Blank(position: Int, maxLength: Int)
  object Blank {
    implicit val reads: Reads[Blank] = (
      (__ \ "position").read[Int] and
      (__ \ "maxLength").read[Int]
    )(Blank.apply _)

    implicit val writes: Writes[Blank] = (
      (__ \ "position").write[Int] and
      (__ \ "maxLength").write[Int]
    )(unlift(Blank.unapply))
  }

  implicit val reads: Reads[BlanksQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "text").read[String] and
    (__ \ "inputs").read[IndexedSeq[Blank]]
  )(BlanksQuestion.apply _)

  implicit val writes = new Writes[BlanksQuestion] {
    def writes(question: BlanksQuestion) = {
      Json.obj(
        "title" -> question.title,
        "description" -> question.description,
        "text" -> question.text,
        "inputs" -> question.inputs,
        "type" -> question.questionType
      )
    }
  }
}

object MultipleChoiceQuestion {
  implicit val reads: Reads[MultipleChoiceQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[String]] and
    (__ \ "correct").read[IndexedSeq[Int]] and
    (__ \ "singleAnswer").read[Boolean]
  )(MultipleChoiceQuestion.apply _)

  implicit val writes = new Writes[MultipleChoiceQuestion] {
    def writes(question: MultipleChoiceQuestion) = {
      Json.obj(
        "title" -> question.title,
        "description" -> question.description,
        "choices" -> question.choices,
        "correct" -> question.correct,
        "singleAnswer" -> question.singleAnswer,
        "type" -> question.questionType
      )
    }
  }
}

object OrderingQuestion {
  implicit val reads: Reads[OrderingQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[String]]
  )(OrderingQuestion.apply _)

  implicit val writes = new Writes[OrderingQuestion] {
    def writes(question: OrderingQuestion) = {
      Json.obj(
        "title" -> question.title,
        "description" -> question.description,
        "choices" -> question.choices,
        "type" -> question.questionType
      )
    }
  }
}

object MatchingQuestion {
  case class Match(left: String, right: String)
  object Match {
    implicit val reads: Reads[Match] = (
      (__ \ "left").read[String] and
      (__ \ "right").read[String]
    )(Match.apply _)

    implicit val writes: Writes[Match] = (
      (__ \ "left").write[String] and
      (__ \ "right").write[String]
    )(unlift(Match.unapply))
  }

  implicit val reads: Reads[MatchingQuestion] = (
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "choices").read[IndexedSeq[Match]]
  )(MatchingQuestion.apply _)

  implicit val writes = new Writes[MatchingQuestion] {
    def writes(question: MatchingQuestion) = {
      Json.obj(
        "title" -> question.title,
        "description" -> question.description,
        "choices" -> question.choices,
        "type" -> question.questionType
      )
    }
  }
}
