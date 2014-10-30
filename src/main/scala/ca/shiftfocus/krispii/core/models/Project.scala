package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class Project(
  id: UUID = UUID.random,
  version: Long = 0,
  name: String,
  slug: String,
  description: String,
  parts: IndexedSeq[Part],
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Project {

  def apply(row: RowData): Project = {
    Project(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      row("description").asInstanceOf[String],
      IndexedSeq[Part](),
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val projectReads: Reads[Project] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String] and
    (__ \ "parts").read[IndexedSeq[Part]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Project.apply(_: UUID, _: Long, _: String, _: String, _: String, _: IndexedSeq[Part], _: Option[DateTime], _: Option[DateTime]))

  implicit val projectWrites: Writes[Project] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "slug").write[String] and
    (__ \ "description").write[String] and
    (__ \ "parts").write[IndexedSeq[Part]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Project.unapply))

}

case class ProjectPost(
  name: String,
  slug: String,
  description: String
)
object ProjectPost {
  implicit val projectPostReads = (
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String]
  )(ProjectPost.apply _)
}

case class ProjectPut(
  version: Long,
  name: String,
  slug: String,
  description: String
)
object ProjectPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String]
  )(ProjectPut.apply _)
}
