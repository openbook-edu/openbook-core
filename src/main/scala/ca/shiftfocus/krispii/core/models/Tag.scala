package ca.shiftfocus.krispii.core.models

import java.util.UUID

import ca.shiftfocus.krispii.core.helpers.NaturalOrderComparator
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Tag(
    id: UUID = UUID.randomUUID(),
    version: Long = 1L,
    isAdmin: Boolean = false,
    isHidden: Boolean = false,
    isPrivate: Boolean = false,
    name: String,
    lang: String,
    category: Option[String],
    frequency: Int
) extends Ordered[Tag] {

  /**
   * Natural order tag name comparation
   * @param that
   * @return
   */
  def compare(that: Tag): Int = {
    val comp = new NaturalOrderComparator
    comp.compare(this.name, that.name)
  }
}

object TaggableEntities {
  val project = "project"
  val organization = "organization"
  val user = "user"
  val plan = "plan"
}

object Tag {
  implicit val tagWrites: Writes[Tag] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "isAdmin").write[Boolean] and
    (__ \ "isHidden").write[Boolean] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "name").write[String] and
    (__ \ "lang").write[String] and
    (__ \ "category").writeNullable[String] and
    (__ \ "frequency").write[Int]
  )(unlift(Tag.unapply))

  implicit val tagReads: Reads[Tag] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "isAdmin").read[Boolean] and
    (__ \ "isHidden").read[Boolean] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "name").read[String] and
    (__ \ "lang").read[String] and
    (__ \ "category").readNullable[String] and
    (__ \ "frequency").read[Int]
  )(Tag.apply _)
}

