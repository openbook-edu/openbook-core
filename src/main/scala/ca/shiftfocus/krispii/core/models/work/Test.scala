package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Test(
    id: UUID = UUID.randomUUID,
    examId: UUID,
    teamId: Option[UUID] = None,
    name: String,
    version: Long = 1L,
    grade: String = "", // initially empty, in contrast with Work
    comments: String = "",
    origResponse: UUID, // PDF/image component
    archived: Boolean = false,
    deleted: Boolean = false,
    scores: IndexedSeq[Score] = IndexedSeq.empty[Score],
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String =
    s"${name}: ${grade}"
}

object Test {
  implicit val testReads: Reads[Test] = (
    (__ \ "id").read[UUID] and
    (__ \ "examId").read[UUID] and
    (__ \ "teamId").readNullable[UUID] and
    (__ \ "name").read[String] and
    (__ \ "version").read[Long] and
    (__ \ "grade").read[String] and
    (__ \ "comments").read[String] and
    (__ \ "origResponse").read[UUID] and
    (__ \ "archived").read[Boolean] and
    (__ \ "deleted").read[Boolean] and
    (__ \ "scores").read[IndexedSeq[Score]] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(Test.apply _)

  implicit val testWrites: Writes[Test] = (
    (__ \ "id").write[UUID] and
    (__ \ "examId").write[UUID] and
    (__ \ "teamId").writeNullable[UUID] and
    (__ \ "name").write[String] and
    (__ \ "version").write[Long] and
    (__ \ "grade").write[String] and
    (__ \ "comments").write[String] and
    (__ \ "origResponse").write[UUID] and
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "scores").write[IndexedSeq[Score]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Test.unapply))
}
