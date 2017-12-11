package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.EmailChangeRequest
import com.github.mauricio.async.db.{ Connection, RowData }

import scala.concurrent.Future
import scalaz.\/

class EmailChangeRepositoryPostgres extends EmailChangeRepository with PostgresRepository[EmailChangeRequest] {

  override val entityName = "EmailChangeRequest"
  val Table = "email_change_requests"
  val Fields = "user_id, requested_email, token"

  override def constructor(row: RowData): EmailChangeRequest = {
    EmailChangeRequest(
      userId = row("user_id").asInstanceOf[UUID],
      requestedEmail = row("requested_email").asInstanceOf[String],
      token = row("token").asInstanceOf[String]
    )
  }

  /**
   * Find an e-mail change request by user ID.
   *
   * @param userId
   * @return
   */
  override def find(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]] = {
    val SelectOne = s"SELECT ${Fields} FROM ${Table} WHERE user_id = ?"
    queryOne(SelectOne, Array(userId))
  }

  /**
   * Find an e-mail change request by e-mail address.
   *
   * @param email
   * @param conn
   * @return
   */
  override def find(email: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]] = {
    val selectOneByEmail = s"SELECT ${Fields} FROM ${Table} WHERE requested_email = ?"
    queryOne(selectOneByEmail, Array(email))
  }

  /**
   * Insert a new e-mail change request.
   *
   * @param ecr
   * @return
   */
  override def insert(ecr: EmailChangeRequest)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]] = {
    val Insert = s"INSERT INTO ${Table} (${Fields}) VALUES (?, ?, ?) RETURNING $Fields"
    queryOne(Insert, Seq[Any](ecr.userId, ecr.requestedEmail, ecr.token))
  }

  /**
   * Delete an e-mail change request.
   *
   * @param ecr
   * @return
   */
  override def delete(ecr: EmailChangeRequest)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]] = {
    val Delete = s"DELETE FROM ${Table} WHERE user_id = ? returning $Fields"
    queryOne(Delete, Seq(ecr.userId))
  }
}
