package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsValue, _ }

case class Gfile(
  id: UUID = UUID.randomUUID(),
  workId: UUID,
  fileId: String,
  mimeType: String,
  fileType: String,
  fileName: String,
  embedUrl: String,
  url: String,
  sharedEmail: Option[String],
  // Save revision id when work was marked as done
  revisionId: Option[String] = None,
  createdAt: DateTime = new DateTime
)

object Gfile {
  implicit val writes: Writes[Gfile] = (
    (__ \ "id").write[UUID] and
    (__ \ "workId").write[UUID] and
    (__ \ "fileId").write[String] and
    (__ \ "mimeType").write[String] and
    (__ \ "fileType").write[String] and
    (__ \ "fileName").write[String] and
    (__ \ "embedUrl").write[String] and
    (__ \ "url").write[String] and
    (__ \ "sharedEmail").writeNullable[String] and
    (__ \ "revisionId").writeNullable[String] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(Gfile.unapply))
}
