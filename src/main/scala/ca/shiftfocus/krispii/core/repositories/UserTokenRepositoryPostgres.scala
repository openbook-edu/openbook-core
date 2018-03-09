package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.UserToken
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{ \/ }

class UserTokenRepositoryPostgres extends UserTokenRepository with PostgresRepository[UserToken] {

  override val entityName = "UserToken"
  val Fields = "user_id, nonce, token_type, created_at"

  override def constructor(row: RowData): UserToken = UserToken(
    userId = row("user_id").asInstanceOf[UUID],
    token = row("nonce").asInstanceOf[String],
    tokenType = row("token_type").asInstanceOf[String],
    createdAt = row("created_at").asInstanceOf[DateTime]
  )

  /**
   * Find one user token by user.
   *
   * @param userId the id of the token's owner
   * @return
   */
  override def find(userId: UUID, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]] = {
    val SelectOne = s"""SELECT $Fields  FROM user_tokens WHERE user_id = ? and token_type = ?"""
    queryOne(SelectOne, Seq[Any](userId.toString, tokenType))
  }

  def findTokenByNonce(nonce: String, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]] = {
    val SelectOne =
      s"""SELECT $Fields FROM user_tokens WHERE nonce = ? and token_type = ?""".stripMargin
    queryOne(SelectOne, Seq[Any](nonce, tokenType))
  }

  /**
   * Insert a new user token.
   *
   * @param userId the id of the token's owner
   * @param nonce the secure token identifier
   * @return
   */
  override def insert(userId: UUID, nonce: String, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Insert = s"INSERT INTO user_tokens (user_id, nonce, token_type, created_at) VALUES (?, ?, ?, ?) RETURNING $Fields"
    queryOne(Insert, Seq[Any](userId, nonce, tokenType, new DateTime))
  }

  /**
   * Update an existing user token (when re-sending the e-mail, this should be used
   * to store the newly generated nonce.
   *
   * @param userId the id of the token's owner
   * @param nonce the secure token identifier
   * @return
   */
  override def update(userId: UUID, nonce: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Update = s"UPDATE user_tokens SET nonce = ? WHERE user_id = ? returning $Fields"
    queryOne(Update, Seq[Any](userId, nonce))
  }

  /**
   * Delete a token.
   *
   * @param userId the id of the user requesting activation
   * @return
   */
  override def delete(userId: UUID, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]] = {
    val Delete = s"DELETE FROM user_tokens WHERE user_id = ? and token_type = ? returning $Fields"
    queryOne(Delete, Seq[Any](userId, tokenType))
  }

}