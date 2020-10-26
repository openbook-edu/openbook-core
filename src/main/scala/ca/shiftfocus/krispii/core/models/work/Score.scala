package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Score(
    id: UUID = UUID.randomUUID,
    testId: UUID,
    scorerId: UUID,
    version: Long = 1L,
    origComments: String = "",
    addComments: String = "", // superseded by test.comments, but left in here just in case
    origGrade: String = "", // initially empty, in contrast with Work
    grade: String = "", // in case we want to keep a consensus-finding process
    isVisible: Boolean = false,
    /* will only be saved separate from the original versions in Test resp. Exam
       if the scorer has actually made changes to the PDFs etc. */
    examFile: Option[UUID] = None, // will usually be an annotated PDF component
    rubricFile: Option[UUID] = None, // could be a rubric, image or PDF component
    archived: Boolean = false,
    deleted: Boolean = false,
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String = {
    (origGrade, grade) match {
      case ("", "") => "None"
      case (origGrade, "") => origGrade
      case _ => grade
    }
  }
}

object Score {
  implicit val scoreReads: Reads[Score] = (
    (__ \ "id").read[UUID] and
    (__ \ "testId").read[UUID] and
    (__ \ "scorerId").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "origComments").read[String] and
    (__ \ "addComments").read[String] and
    (__ \ "origGrade").read[String] and
    (__ \ "grade").read[String] and
    (__ \ "isVisible").read[Boolean] and
    (__ \ "examFile").readNullable[UUID] and
    (__ \ "rubricFile").readNullable[UUID] and
    (__ \ "archived").read[Boolean] and
    (__ \ "deleted").read[Boolean] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(Score.apply _)

  implicit val scoreWrites: Writes[Score] = (
    (__ \ "id").write[UUID] and
    (__ \ "testId").write[UUID] and
    (__ \ "scorerId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "origComments").write[String] and
    (__ \ "addComments").write[String] and
    (__ \ "origGrade").write[String] and
    (__ \ "grade").write[String] and
    (__ \ "isVisible").write[Boolean] and
    (__ \ "examFile").writeNullable[UUID] and
    (__ \ "rubricFile").writeNullable[UUID] and
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Score.unapply))
}
