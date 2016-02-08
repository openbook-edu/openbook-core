package ca.shiftfocus.krispii.core.models

/**
 * Created by vzaytseva on 21/01/16.
 */

import java.util.UUID

import ca.shiftfocus.krispii.core.models.document.Document
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class ProjectScratchpadOutOfDateException(msg: String) extends Exception
case class ProjectScratchpadAlreadyExistsException(msg: String) extends Exception
case class ProjectScratchpadDisabledException(msg: String) extends Exception

case class ProjectScratchpad(
  userId: UUID,
  projectId: UUID,
  version: Long = 1L,
  documentId: UUID,
  document: Option[Document] = None,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object ProjectScratchpad {

  implicit val projectWrites: Writes[ProjectScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "projectId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "documentId").write[UUID] and
    (__ \ "document").writeNullable[Document] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(ProjectScratchpad.unapply _))

}
