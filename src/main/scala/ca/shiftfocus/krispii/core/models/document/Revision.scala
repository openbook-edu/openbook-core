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
  author: User,
  delta: Delta,
  createdAt: Option[DateTime]
)

object Revision {

  implicit val reads: Reads[Revision] = (
    (__ \ "documentId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "author").read[User] and
      (__ \ "delta").read[Delta] and
      (__ \ "createdAt").readNullable[DateTime]
    )(Revision.apply(_: UUID, _: Long, _: User, _: Delta, _: Option[DateTime]))

  implicit val writes: Writes[Revision] = (
    (__ \ "documentId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "author").write[User] and
      (__ \ "delta").write[Delta] and
      (__ \ "createdAt").writeNullable[DateTime]
    )(unlift(Revision.unapply))

  def apply(row: RowData)(author: User): Revision = {
    Revision(
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      version = row("version").asInstanceOf[Long],
      author = author,
      delta = Json.parse(row("delta").asInstanceOf[String]).as[Delta],
      createdAt = row("created_at").asInstanceOf[Option[DateTime]]
    )
  }

}