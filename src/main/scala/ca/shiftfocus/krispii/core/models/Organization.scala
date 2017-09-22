package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json, Writes }

case class Organization(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  title: String,
  adminEmail: Option[String] = None,
  tags: IndexedSeq[Tag] = IndexedSeq.empty[Tag],
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
        "adminEmail" -> organization.adminEmail,
        "tags" -> organization.tags,
        "createdAt" -> organization.createdAt,
        "updatedAt" -> organization.updatedAt
      )
    }
  }
}

