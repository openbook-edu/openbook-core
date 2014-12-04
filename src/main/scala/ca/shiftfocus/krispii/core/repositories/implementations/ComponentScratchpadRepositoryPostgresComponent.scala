package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

trait ComponentScratchpadRepositoryPostgresComponent extends ComponentScratchpadRepositoryComponent {
  self: PostgresDB =>

  override val componentScratchpadRepository: ComponentScratchpadRepository = new ComponentScratchpadRepositoryPSQL

  private class ComponentScratchpadRepositoryPSQL extends ComponentScratchpadRepository {
    def fields = Seq("notes")
    def table = "component_notes"
    def orderBy = "surname ASC, givenname ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (user_id, component_id, version, created_at, updated_at, $extraFields)
        VALUES (?, ?, 1, ?, ?, $questions)
        RETURNING version
      """
    }

    val Update = {
      val extraFields = fields.map(" " + _ + " = ? ").mkString(",")
      s"""
        UPDATE $table
        SET $extraFields , version = ?, updated_at = ?
        WHERE user_id = ?
          AND component_id = ?
          AND version = ?
        RETURNING version
      """
    }

    val SelectAll = s"""
      SELECT user_id, component_id, version, created_at, updated_at, $fieldsText
      FROM $table
      ORDER BY id asc
    """

    val SelectOne = s"""
      SELECT user_id, component_id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND component_id = ?
      LIMIT 1
    """

    val SelectRevisionsById = s"""
      SELECT user_id, component_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND component_id = ?
      ORDER BY revision DESC
    """

    val SelectLatest = s"""
      SELECT user_id, component_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND component_id = ?
      ORDER BY revision DESC
      LIMIT 1
    """

    val SelectLatestByProject = s"""
      SELECT user_id, component_id, revision, task_notes.version, task_notes.created_at, task_notes.updated_at, $fieldsText
      FROM $table, parts, projects, tasks
      WHERE user_id = ?
        AND projects.id = ?
        AND parts.id = tasks.part_id
        AND projects.id = parts.project_id
        AND task_notes.component_id = tasks.id
        AND revision = (SELECT MAX(revision) FROM task_notes WHERE user_id= ? AND component_id=tasks.id)
    """

    val SelectByUserId = s"""
      SELECT sr1.user_id, sr1.component_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.notes
      FROM task_notes sr1 LEFT JOIN task_notes sr2
        ON (sr1.user_id = sr2.user_id AND sr1.component_id < sr2.component_id)
      WHERE user_id = ?
        AND sr1.component_id IS NULL
      ORDER BY revision DESC
    """

    val SelectByTaskId = s"""
      SELECT sr1.user_id, sr1.component_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.notes
      FROM task_notes sr1 LEFT JOIN task_notes sr2
        ON (sr1.component_id = sr2.component_id AND sr1.user_id < sr2.user_id)
      WHERE component_id = ?
        AND sr1.user_id IS NULL
      ORDER BY revision DESC
    """

    val Delete = s"""
      DELETE FROM $table
      WHERE user_id = ?
        AND component_id = ?
        AND version = ?
    """

    val Purge = s"""
      DELETE FROM $table
      WHERE user_id = ?
        AND component_id = ?
        AND version = ?
    """

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(user: User, component: Component, revision: Long)(implicit conn: Connection): Future[Option[ComponentScratchpad]] = {
      for {
        result <- conn.sendPreparedStatement(SelectOne, Array[Any](user.id.bytes, component.id.bytes, revision))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(ComponentScratchpad(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(user: User, component: Component)(implicit conn: Connection): Future[Option[ComponentScratchpad]] = {
      for {
        result <- conn.sendPreparedStatement(SelectLatest, Array[Any](user.id.bytes, component.id.bytes))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(ComponentScratchpad(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Select rows by their user ID.
     *
     * @param userId the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(user: User, component: Component)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectRevisionsById, Array[Any](user.id.bytes, component.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => ComponentScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Select rows by their user ID.
     *
     * @param userId the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectLatestByProject, Array[Any](user.id.bytes, project.id.bytes, user.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => ComponentScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Select rows by their user ID.
     *
     * @param userId the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(user: User)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByUserId, Array[Any](user.id.bytes))
      } yield queryResult.rows.get.map {
        item: RowData => ComponentScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Select rows by their task ID.
     *
     * @param taskId the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(component: Component)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByTaskId, Array[Any](component.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => ComponentScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Insert a new ComponentScratchpad. Used to create new scratchpads, and to insert new
     * revisions to existing pads. Note that the primary key comprises the user's ID,
     * the task's ID, and the revision number, so each revision is a separate entry in
     * the database.
     *
     * @param ComponentScratchpad the [[ComponentScratchpad]] object to be inserted.
     * @return the newly created [[ComponentScratchpad]]
     */
    override def insert(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[ComponentScratchpad] = {
      for {
        result <- conn.sendPreparedStatement(Insert, Array(
          componentScratchpad.userId.bytes,
          componentScratchpad.componentId.bytes,
          componentScratchpad.revision,
          new DateTime,
          new DateTime,
          componentScratchpad.content
        ))
      }
      yield ComponentScratchpad(result.rows.get.head)
    }.recover {
      case exception => throw exception
    }

    /**
     * Update an existing [[ComponentScratchpad]] revision. This always updates a specific
     * revision, since the primary key comprises user ID, task ID, and revision number.
     * Each revision has its own versioning w.r.t. optimistic offline lock.
     *
     * @param ComponentScratchpad the [[ComponentScratchpad]] object to be inserted.
     * @return the newly created [[ComponentScratchpad]]
     */
    override def update(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[ComponentScratchpad] = {
      for {
        result <- conn.sendPreparedStatement(Update, Array(
          componentScratchpad.content,
          (componentScratchpad.version + 1),
          new DateTime,
          componentScratchpad.userId.bytes,
          componentScratchpad.componentId.bytes,
          componentScratchpad.revision,
          componentScratchpad.version
        ))
      }
      yield ComponentScratchpad(result.rows.get.head)
    }.recover {
      case exception => throw exception
    }

    /**
     * Delete a component scratchpad.
     *
     * @param id
     * @return
     */
    override def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[Boolean] = {
      for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(
          componentScratchpad.userId.bytes,
          componentScratchpad.componentId.bytes,
          componentScratchpad.version))
      }
      yield { queryResult.rowsAffected > 0 }
    }
  }
}
