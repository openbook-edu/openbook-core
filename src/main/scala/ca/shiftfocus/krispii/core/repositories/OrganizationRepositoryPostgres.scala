package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class OrganizationRepositoryPostgres extends OrganizationRepository with PostgresRepository[Organization] {
  override val entityName = "Organization"

  override def constructor(row: RowData): Organization = {
    Organization(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      title = row("title").asInstanceOf[String],
      adminEmail = Option(row("admin_email").asInstanceOf[String]) match {
      case Some(adminEmail) => Some(adminEmail)
      case _ => None
    },
      tags = IndexedSeq.empty[Tag],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, title, admin_email, created_at, updated_at"
  val Table = "organizations"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  val FieldsWithQMarks = Fields.split(", ").map({ field => s"${field} = ?" }).mkString(", ")

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET $FieldsWithQMarks
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  def find(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    queryOne(SelectOne, Seq[Any](organizationId))
  }

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]] = {
    queryList(SelectAll)
  }

  def insert(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](
      organization.id, organization.version, organization.title,
      organization.adminEmail, organization.createdAt, organization.updatedAt
    )

    queryOne(Insert, params)
  }

  def update(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](
      organization.id, organization.version + 1, organization.title,
      organization.adminEmail, organization.createdAt, new DateTime(),
      organization.id, organization.version
    )

    queryOne(Update, params)
  }

  def delete(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](organization.id, organization.version)

    queryOne(Delete, params)
  }
}
