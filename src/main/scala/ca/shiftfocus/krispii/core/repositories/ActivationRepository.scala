package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import _root_.helpers.UUID
import _root_.helpers.expect._
import ca.shiftfocus.krispii.core.models.UserToken
import models.UserToken
import com.github.mauricio.async.db.{ RowData, Connection }


/**
  * Handles the CRUD operations related to the token of a new user when signing up.
  * Created by ryanez on 11/02/16.
  */
trait ActivationRepository extends Repository {
  def constructor(row: RowData): UserToken
  def find(userId: UUID): ExpectReader[Connection, UserToken]
  def find(email: String): ExpectReader[Connection, UserToken]
  def insert(userId: UUID, nonce: String): ExpectReader[Connection, UserToken]
  def update(userId: UUID, nonce: String): ExpectReader[Connection, UserToken]
  def delete(userId: UUID): ExpectReader[Connection, UserToken]
}
