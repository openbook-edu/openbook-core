package ca.shiftfocus.krispii.core.models.document

import ca.shiftfocus.krispii.core.models.User
import java.util.UUID
import java.security.MessageDigest
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import ws.kahn.ot._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class Revision(
    documentId: UUID,
    version: Long,
    authorId: UUID,
    delta: Delta,
    createdAt: DateTime = new DateTime
) {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherRevision: Revision => {
        this.documentId == anotherRevision.documentId &&
          this.version == anotherRevision.version &&
          this.authorId == anotherRevision.authorId &&
          this.delta == anotherRevision.delta
      }
      case _ => false
    }
  }
}

object Revision {

  implicit val writes: Writes[Revision] = (
    (__ \ "documentId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "authorId").write[UUID] and
    (__ \ "delta").write[Delta] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(Revision.unapply))

}
