package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.UserToken
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.{ -\/, \/-, \/ }

class ActivationRepositoryPostgres extends ActivationRepository with PostgresRepository[UserToken] {

  override val entityName = "Activation"
  val Fields = "user_id, nonce, created_at"

  override def constructor(row: RowData): UserToken = UserToken(
    userId = row("user_id").asInstanceOf[UUID],
    token = row("nonce").asInstanceOf[String]
  )

  /**
   * Find one activation by its user.
   *
   * @param userId the id of the user requesting activation
   * @return
   */
  override def find(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]] = {
    val SelectOne = "SELECT user_id, nonce FROM activations WHERE user_id = ?"
    queryOne(SelectOne, Seq[Any](userId.toString))
  }

  /**
   * Find one activation by the user's e-mail address.
   *
   * @param email the email of the user requesting activation
   * @return
   */
  override def find(email: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]] = {
    val SelectOneByEmail = "SELECT  user_id, nonce FROM users, activations WHERE email = ? and user_id = users.id"
    queryOne(SelectOneByEmail, Seq[Any](email))
  }

  /**
   * Insert a new activation.
   *
   * @param userId the id of the user requesting activation
   * @param nonce the secure token identifying this user's activation
   * @return
   */
  override def insert(userId: UUID, nonce: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Insert = s"INSERT INTO activations (user_id, nonce) VALUES (?, ?) RETURNING $Fields"
    queryOne(Insert, Seq[Any](userId, nonce))
  }

  /**
   * Update an existing activation (when re-sending the e-mail, this should be used
   * to store the newly generated nonce.
   *
   * @param userId the id of the user requesting activation
   * @param nonce the secure token identifying this user's activation
   * @return
   */
  override def update(userId: UUID, nonce: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Update = s"UPDATE activations SET nonce = ? WHERE user_id = ? returning $Fields"
    queryOne(Update, Seq[Any](userId, nonce))
  }

  /**
   * Delete an activation.
   *
   * @param userId the id of the user requesting activation
   * @return
   */
  override def delete(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Delete = s"DELETE FROM activations WHERE user_id = ? returning $Fields"
    queryOne(Delete, Seq[Any](userId))
  }

}