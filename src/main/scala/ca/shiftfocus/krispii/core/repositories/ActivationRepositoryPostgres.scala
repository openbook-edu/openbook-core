package ca.shiftfocus.krispii.core.repositories


import java.util.UUID

import ca.shiftfocus.krispii.core.models.UserToken
import com.github.mauricio.async.db.{Connection, RowData}

import scalaz.{ -\/, \/-, \/ }

class ActivationRepositoryPostgres extends ActivationRepository with PostgresRepository[UserToken] {

  override val entityName = "Activation"
  val Fields = "user_id, nonce"

  override def constructor(row: RowData): UserToken = {
    UserToken(
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      row("nonce").asInstanceOf[String]
    )
  }

  /**
    * Find one activation by its user.
    *
    * @param userId the id of the user requesting activation
    * @return
    */
  override def find(userId: UUID): ExpectReader[Connection, UserToken] = {
    val SelectOne = "SELECT user_id, nonce FROM activations WHERE user_id = ?"
    queryOne(SelectOne, Array(userId.bytes))
  }

  /**
    * Find one activation by the user's e-mail address.
    *
    * @param email the email of the user requesting activation
    * @return
    */
  override def find(email: String): ExpectReader[Connection, UserToken] = {
    val SelectOneByEmail = "SELECT  user_id, nonce FROM users, activations WHERE email = ? and user_id = users.id"
    queryOne(SelectOneByEmail, Array(email))
  }

  /**
    * Insert a new activation.
    *
    * @param userId the id of the user requesting activation
    * @param nonce the secure token identifying this user's activation
    * @return
    */
  override def insert(userId: UUID, nonce: String): ExpectReader[Connection, UserToken] = {
    val Insert = s"INSERT INTO activations (user_id, nonce) VALUES (?, ?) RETURNING $Fields"
    queryOne(Insert, Array(userId.bytes, nonce))
  }

  /**
    * Update an existing activation (when re-sending the e-mail, this should be used
    * to store the newly generated nonce.
    *
    * @param userId the id of the user requesting activation
    * @param nonce the secure token identifying this user's activation
    * @return
    */
  override def update(userId: UUID, nonce: String): ExpectReader[Connection, UserToken] = {
    val Update = s"UPDATE activations SET nonce = ? WHERE user_id = ? returning $Fields"
    queryOne(Update, Array(userId.bytes, nonce))
  }

  /**
    * Delete an activation.
    *
    * @param userId the id of the user requesting activation
    * @return
    */
  override def delete(userId: UUID): ExpectReader[Connection, UserToken] = {
    val Delete = s"DELETE FROM activations WHERE user_id = ? returning $Fields"
    queryOne(Delete, Array(userId.bytes))
  }

}