package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.core.models._
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.core.services.datasource.PostgresDB

trait JournalRepositoryPostgresComponent extends JournalRepositoryComponent {
  self: PostgresDB =>

  override val journalRepository: JournalRepository = new JournalRepositoryPSQL

  private class JournalRepositoryPSQL extends JournalRepository {

    def fields = Seq("remote_address", "request_uri", "user_agent", "user_id", "message")
    def table = "logbook"
    def orderBy = "id ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY $orderBy
    """

    val SelectAllInDateRange = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
        AND created_at >= ?
        AND created_at <= ?
      ORDER BY $orderBy
    """

    val SelectByUserId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
        AND user_id = ?
      ORDER BY $orderBy
    """

    val SelectByUserIdInDateRange = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
        AND user_id = ?
        AND created_at >= ?
        AND created_at <= ?
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
        AND status = 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
        VALUES (?, 1, 1, ?, ?, $questions)
        RETURNING id, version, created_at, updated_at, $fieldsText
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
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      UPDATE $table SET status = 0 WHERE id = ? AND version = ?
    """

    /**
     * Find all journal entries.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned tasks
     */
    override def list(implicit conn: Connection): Future[IndexedSeq[JournalEntry]] = {
      list(None, None)
    }
    override def list(startDateOption: Option[DateTime],
                      endDateOption: Option[DateTime]
    )(implicit conn: Connection): Future[IndexedSeq[JournalEntry]] = {
      (startDateOption, endDateOption) match {
        case (Some(startDate), Some(endDate)) => {
          conn.sendPreparedStatement(SelectAllInDateRange, Array[Any](startDate, endDate)).map {
            queryResult =>
              queryResult.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (Some(startDate), _) => {
          conn.sendPreparedStatement(SelectAllInDateRange, Array[Any](startDate, (new DateTime).withYear(3000))).map {
            queryResult =>
              queryResult.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (None, Some(endDate)) => {
          conn.sendPreparedStatement(SelectAllInDateRange, Array[Any]((new DateTime).withYear(1970), endDate)).map {
            queryResult =>
              queryResult.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (None, None) => {
          conn.sendQuery(SelectAll).map { queryResult =>
            queryResult.rows.get.map {
              item: RowData => JournalEntry(item)
            }
          }
        }
      }
    }.recover {
      case exception => {
        throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def list(user: User)(implicit conn: Connection): Future[IndexedSeq[JournalEntry]] = {
      list(user, None, None)
    }
    override def list(user: User,
                      startDateOption: Option[DateTime],
                      endDateOption: Option[DateTime]
    )(implicit conn: Connection): Future[IndexedSeq[JournalEntry]] = {
      (startDateOption, endDateOption) match {
        case (Some(startDate), Some(endDate)) => {
          conn.sendPreparedStatement(SelectByUserIdInDateRange, Array[Any](startDate, endDate)).map {
            result =>
              result.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (Some(startDate), _) => {
          conn.sendPreparedStatement(SelectByUserIdInDateRange, Array[Any](startDate, (new DateTime).withYear(3000))).map {
            result =>
              result.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (None, Some(endDate)) => {
          conn.sendPreparedStatement(SelectByUserIdInDateRange, Array[Any]((new DateTime).withYear(1970), endDate)).map {
            result =>
              result.rows.get.map {
                item: RowData => JournalEntry(item)
              }
          }
        }
        case (None, None) => {
          conn.sendPreparedStatement(SelectByUserId, Array[Any](user.id)).map { result =>
            result.rows.get.map {
              item: RowData => JournalEntry(item)
            }
          }
        }
      }
    }.recover {
      case exception => {
        throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def find(id: UUID)(implicit conn: Connection): Future[Option[JournalEntry]] = {
      conn.sendPreparedStatement(SelectOne, Array[Any](id)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(JournalEntry(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Insert a new entry into the Journal.
     *
     * @param journalEntry the journal entry to insert
     * @return the newly created journal entry
     */
    override def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[JournalEntry] = {
      for {
        result <- conn.sendPreparedStatement(Insert, Array(
          journalEntry.id,
          new DateTime,
          new DateTime,
          journalEntry.remoteAddress,
          journalEntry.requestUri,
          journalEntry.userAgent,
          journalEntry.userId,
          journalEntry.message
        ))
      }
      yield JournalEntry(result.rows.get.head)
    }.recover {
      case exception => {
        throw exception
      }
    }

    /**
     * Save a Role row.
     *
     * @return id of the saved/new role.
     */
    override def update(journalEntry: JournalEntry)(implicit conn: Connection): Future[JournalEntry] = {
      for {
        result <- conn.sendPreparedStatement(Update, Array(
          journalEntry.remoteAddress,
          journalEntry.requestUri,
          journalEntry.userAgent,
          journalEntry.userId,
          journalEntry.message,
          new DateTime,
          journalEntry.id,
          journalEntry.version
        ))
      }
      yield JournalEntry(result.rows.get.head)
    }.recover {
      case exception => {
        throw exception
      }
    }
  }
}
