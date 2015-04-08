package ca.shiftfocus.krispii.core.models.document

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.uuid.UUID
import java.security.MessageDigest
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import ws.kahn.ot._

case class Revision(
  documentId: UUID,
  version: Long,
  authorId: UUID,
  delta: Delta,
  createdAt: DateTime = new DateTime
)

object Revision {

  implicit val writes: Writes[Revision] = (
    (__ \ "documentId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "authorId").write[UUID] and
      (__ \ "delta").write[Delta] and
      (__ \ "createdAt").write[DateTime]
    )(unlift(Revision.unapply))

}