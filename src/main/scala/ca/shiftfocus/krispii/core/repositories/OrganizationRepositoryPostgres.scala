package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaz.{ -\/, \/, \/- }

class OrganizationRepositoryPostgres extends OrganizationRepository with PostgresRepository[Organization] {
  override val entityName = "Organization"

  override def constructor(row: RowData): Organization = {
    Organization(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      title = row("title").asInstanceOf[String],
      tags = IndexedSeq.empty[Tag],
      admins = Try(row("admins").asInstanceOf[String].split(",").to[IndexedSeq]).toOption.getOrElse(IndexedSeq.empty[String]),
      members = Try(row("members").asInstanceOf[String].split(",").to[IndexedSeq]).toOption.getOrElse(IndexedSeq.empty[String]),
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, title, created_at, updated_at"
  val Table = "organizations"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  val FieldsWithQMarks = Fields.split(", ").map({ field => s"${field} = ?" }).mkString(", ")

  val MembersField =
    s"""
       |string_agg(member_email, ',') AS members
     """.stripMargin

  val AdminsField =
    s"""
       |string_agg(admin_email, ',') AS admins
     """.stripMargin

  val Join =
    s"""
       |LEFT JOIN organization_members AS om
       |  ON om.organization_id = $Table.id
       |LEFT JOIN organization_admins AS oa
       |  ON oa.organization_id = $Table.id
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields, $MembersField, $AdminsField
       |FROM $Table
       |$Join
       |WHERE id = ?
       |GROUP BY $Table.id
     """.stripMargin

  val SelectAll =
    s"""
       |SELECT $Fields, $MembersField, $AdminsField
       |FROM $Table
       |$Join
       |GROUP BY $Table.id
     """.stripMargin

  val SelectAllByAdminEmail =
    s"""
       |SELECT $Fields, $MembersField, $AdminsField
       |FROM $Table
       |$Join
       |WHERE oa.admin_email = ?
       |GROUP BY $Table.id
     """.stripMargin

  val SelectAllByMemberEmail =
    s"""
       |SELECT $Fields, $MembersField, $AdminsField
       |FROM $Table
       |$Join
       |WHERE om.member_email = ?
       |GROUP BY $Table.id
     """.stripMargin

  def SelectByTags(tags: IndexedSeq[(String, String)], distinct: Boolean): String = {
    var whereClause = ""
    var distinctClause = ""
    val length = tags.length

    tags.zipWithIndex.map {
      case ((tagName, tagLang), index) =>
        whereClause += s"""(tags.name='${tagName}' AND tags.lang='${tagLang}')"""
        if (index != (length - 1)) whereClause += " OR "
    }

    if (whereClause != "") {
      whereClause = "WHERE " + whereClause
    }
    // If tagList is empty, then there should be unexisting condition
    else {
      whereClause = "WHERE false != false"
    }

    if (distinct) {
      distinctClause = s"HAVING COUNT(DISTINCT tags.name) = $length"
    }

    def query(whereClause: String) =
      s"""
        |SELECT $Fields, $MembersField, $AdminsField
        |FROM $Table
        |$Join
        |WHERE id IN (
        |    SELECT organization_id
        |    FROM organization_tags
        |    JOIN tags
        |      ON organization_tags.tag_id = tags.id
        |    ${whereClause}
        |    GROUP BY organization_id
        |    ${distinctClause}
        |)
        |GROUP BY $Table.id
    """.stripMargin

    query(whereClause)
  }

  val AddMember =
    s"""
       |INSERT INTO organization_members (organization_id, member_email)
       |VALUES (?, ?)
       |RETURNING *
     """.stripMargin

  val DeleteMember =
    s"""
       |DELETE FROM organization_members
       |WHERE organization_id = ?
       |  AND member_email = ?
       |RETURNING *
     """.stripMargin

  val AddAdmin =
    s"""
       |INSERT INTO organization_admins (organization_id, admin_email)
       |VALUES (?, ?)
       |RETURNING *
     """.stripMargin

  val DeleteAdmin =
    s"""
       |DELETE FROM organization_admins
       |WHERE organization_id = ?
       |  AND admin_email = ?
       |RETURNING *
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
       |USING
       |  organization_members
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

  def listByAdmin(adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]] = {
    queryList(SelectAllByAdminEmail, Seq[Any](adminEmail))
  }

  def listByMember(memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]] = {
    queryList(SelectAllByMemberEmail, Seq[Any](memberEmail))
  }

  /**
   * List organizations by tags
   *
   * @param tags (tagName:String, tagLang:String)
   * @param distinct Boolean If true each organization should have all listed tags,
   *                 if false organization should have at least one listed tag
   */
  def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]] = {
    val select = SelectByTags(tags, distinct)
    queryList(select)
  }

  def addMember(organization: Organization, memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    for {
      _ <- lift {
        conn.sendPreparedStatement(AddMember, Array[Any](organization.id, memberEmail)).map { result =>
          result.rows match {
            case Some(resultSet) => resultSet.headOption match {
              case Some(firstRow) => \/-(firstRow)
              case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not add member to organization"))
            }
            case None => -\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add member to organization"))
          }
        }.recover {
          case exception: GenericDatabaseException =>
            val fields = exception.errorMessage.fields
            (fields.get('t'), fields.get('n')) match {
              case (Some(table), Some(nField)) if nField endsWith "_pkey" =>
                \/.left(RepositoryError.PrimaryKeyConflict)

              case (Some(table), Some(nField)) if nField endsWith "_key" =>
                \/.left(RepositoryError.UniqueKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 4).mkString), nField))

              case (Some(table), Some(nField)) if nField endsWith "_fkey" =>
                \/.left(RepositoryError.ForeignKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 5).mkString), nField))

              case _ => \/.left(RepositoryError.DatabaseError("Unhandled GenericDataabaseException", Some(exception)))
            }
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }
      }
      organizationWithMember <- lift(queryOne(SelectOne, Seq[Any](organization.id)))
    } yield organizationWithMember
  }

  def deleteMember(organization: Organization, memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    for {
      _ <- lift {
        conn.sendPreparedStatement(DeleteMember, Array[Any](organization.id, memberEmail)).map { result =>
          result.rows match {
            case Some(resultSet) => resultSet.headOption match {
              case Some(firstRow) => \/-(firstRow)
              case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not remove member from organization"))
            }
            case None => -\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove member from organization"))
          }
        }.recover {
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }
      }
      organizationWithoutMember <- lift(queryOne(SelectOne, Seq[Any](organization.id)))
    } yield organizationWithoutMember
  }

  def addAdmin(organization: Organization, adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    for {
      _ <- lift {
        conn.sendPreparedStatement(AddAdmin, Array[Any](organization.id, adminEmail)).map { result =>
          result.rows match {
            case Some(resultSet) => resultSet.headOption match {
              case Some(firstRow) => \/-(firstRow)
              case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not add admin to organization"))
            }
            case None => -\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add admin to organization"))
          }
        }.recover {
          case exception: GenericDatabaseException =>
            val fields = exception.errorMessage.fields
            (fields.get('t'), fields.get('n')) match {
              case (Some(table), Some(nField)) if nField endsWith "_pkey" =>
                \/.left(RepositoryError.PrimaryKeyConflict)

              case (Some(table), Some(nField)) if nField endsWith "_key" =>
                \/.left(RepositoryError.UniqueKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 4).mkString), nField))

              case (Some(table), Some(nField)) if nField endsWith "_fkey" =>
                \/.left(RepositoryError.ForeignKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 5).mkString), nField))

              case _ => \/.left(RepositoryError.DatabaseError("Unhandled GenericDataabaseException", Some(exception)))
            }
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }
      }
      organizationWithAdmin <- lift(queryOne(SelectOne, Seq[Any](organization.id)))
    } yield organizationWithAdmin
  }

  def deleteAdmin(organization: Organization, adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    for {
      _ <- lift {
        conn.sendPreparedStatement(DeleteAdmin, Array[Any](organization.id, adminEmail)).map { result =>
          result.rows match {
            case Some(resultSet) => resultSet.headOption match {
              case Some(firstRow) => \/-(firstRow)
              case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not remove admin from organization"))
            }
            case None => -\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove admin from organization"))
          }
        }.recover {
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }
      }
      organizationWithoutAdmin <- lift(queryOne(SelectOne, Seq[Any](organization.id)))
    } yield organizationWithoutAdmin
  }

  def insert(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](
      organization.id, organization.version, organization.title, organization.createdAt, organization.updatedAt
    )

    queryOne(Insert, params)
  }

  def update(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](
      organization.id, organization.version + 1, organization.title,
      organization.createdAt, new DateTime(),
      organization.id, organization.version
    )

    queryOne(Update, params).map {
      case \/-(org) => \/-(org.copy(
        admins = organization.admins,
        members = organization.members
      ))
      case -\/(error) => -\/(error)
    }
  }

  def delete(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]] = {
    val params = Seq[Any](organization.id, organization.version)

    queryOne(Delete, params).map {
      case \/-(org) => \/-(org.copy(
        admins = organization.admins,
        members = organization.members
      ))
      case -\/(error) => -\/(error)
    }
  }
}
