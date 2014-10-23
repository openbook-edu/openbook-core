package repositories

import com.github.mauricio.async.db.{RowData, Connection, ResultSet}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import scala.concurrent.Future
import play.api.Logger

trait TableDataGateway {

  /**
   * These values are used by the below finder functions, but should be overriden
   * in the implementing class to actually, you know, do something.
   */
  def fields: Seq[String] = Seq()
  val fieldsText = fields.mkString(", ")
  val questions = fields.map(_ => "?").mkString(", ")
  def table = ""
  def orderBy = "id ASC"

  // User CRUD operations
  val Insert = {
    val extraFields = fields.mkString(",")
    val questions = fields.map(_ => "?").mkString(",")
    s"""
      INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
      VALUES (?, 1, 1, ?, ?, $questions)
      RETURNING version
    """
  }

  val Update = {
    val extraFields = fields.map(" " + _ + " = ? ").mkString(",")
    s"""
      UPDATE $table
      SET $extraFields , version = ?, updated_at = ?
      WHERE id = ?
        AND version = ?
        AND status = 1
      RETURNING version
    """
  }

  val SelectAll = s"""
    SELECT id, version, created_at, updated_at, $fieldsText
    FROM $table
    WHERE status = 1
    ORDER BY $orderBy
  """

  val SelectOne = s"""
    SELECT id, version, created_at, updated_at, $fieldsText
    FROM $table
    WHERE id = ?
      AND status = 1
  """

  val Delete = s"""
    UPDATE $table SET status = 0 WHERE id = ? AND version = ?
  """

  val Restore = s"""
    UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
  """

  val Purge = s"""
    DELETE FROM $table WHERE id = ? AND version = ?
  """
}
