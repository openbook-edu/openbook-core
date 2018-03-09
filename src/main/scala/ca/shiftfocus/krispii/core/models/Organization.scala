package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json, Writes }
import play.api.libs.json.JodaWrites._

case class Organization(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  title: String,
  admins: IndexedSeq[String] = IndexedSeq.empty[String],
  tags: IndexedSeq[Tag] = IndexedSeq.empty[Tag],
  members: IndexedSeq[String] = IndexedSeq.empty[String],
  createdAt: DateTime = new DateTime(),
  updatedAt: DateTime = new DateTime()
)

object Organization {
  implicit val organizationWrites = new Writes[Organization] {
    def writes(organization: Organization): JsValue = {
      Json.obj(
        "id" -> organization.id,
        "version" -> organization.version,
        "title" -> organization.title,
        "admins" -> organization.admins,
        "tags" -> organization.tags,
        "members" -> organization.members,
        "createdAt" -> organization.createdAt,
        "updatedAt" -> organization.updatedAt
      )
    }
  }
}

