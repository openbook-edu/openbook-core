package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

trait ComponentScratchpadRepositoryPostgresComponent extends ComponentScratchpadRepositoryComponent {
  self: UserRepositoryComponent with
        ComponentRepositoryComponent with
        PostgresDB =>

  override val componentScratchpadRepository: ComponentScratchpadRepository = new ComponentScratchpadRepositoryPSQL

  private class ComponentScratchpadRepositoryPSQL extends ComponentScratchpadRepository {
    val Table = "component_scratchpads"
    val Fields = "user_id, component_id, version, created_at, updated_at, document_id"
    val QMarks = "?, ?, ?, ?, ?, ?"

    val Insert = {
      s"""
        INSERT INTO $Table ($Fields)
        VALUES ($QMarks)
        RETURNING $Fields
      """
    }

    val Update = {
      s"""
        UPDATE $Table
        SET document_id = ?, version = ?, updated_at = ?
        WHERE user_id = ?
          AND component_id = ?
          AND version = ?
        RETURNING $Fields
      """
    }

    val SelectAllForComponent =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE component_id = ?
       """.stripMargin

    val SelectAllForUser =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE user_id = ?
         |ORDER BY id asc
       """.stripMargin

    val SelectOne = s"""
      SELECT $Fields
      FROM $Table
      WHERE user_id = ?
        AND component_id = ?
      LIMIT 1
    """

    val SelectAllForUserAndProject = s"""
      SELECT $Fields
      FROM $Table, parts, projects, tasks
      WHERE user_id = ?
        AND projects.id = ?
        AND parts.id = tasks.part_id
        AND projects.id = parts.project_id
        AND $Table.component_id = tasks.id
    """

    val Delete = s"""
      DELETE FROM $Table
      WHERE user_id = ?
        AND component_id = ?
        AND version = ?
    """
    
    /**
     * Select rows by their user ID.
     *
     * @param user the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      conn.sendPreparedStatement(SelectAllForUser, Array[Any](user.id.bytes)).map {
        result => buildComponentScratchpadList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Select rows by their task ID.
     *
     * @param component the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(component: Component)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      conn.sendPreparedStatement(SelectAllForComponent, Array[Any](component.id.bytes)).map {
        result => buildComponentScratchpadList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(user: User, component: Component)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      conn.sendPreparedStatement(SelectOne, Array[Any](user.id.bytes, component.id.bytes)).map {
        result => buildComponentScratchpad(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Insert a new ComponentScratchpad. Used to create new scratchpads, and to insert new
     * revisions to existing pads. Note that the primary key comprises the user's ID,
     * the task's ID, and the revision number, so each revision is a separate entry in
     * the database.
     *
     * @param componentScratchpad the [[ComponentScratchpad]] object to be inserted.
     * @return the newly created [[ComponentScratchpad]]
     */
    override def insert(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      conn.sendPreparedStatement(Insert, Array(
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        1L,
        new DateTime,
        new DateTime,
        componentScratchpad.documentId
      )).map {
        result => buildComponentScratchpad(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Update an existing [[ComponentScratchpad]] revision. This always updates a specific
     * revision, since the primary key comprises user ID, task ID, and revision number.
     * Each revision has its own versioning w.r.t. optimistic offline lock.
     *
     * @param componentScratchpad the [[ComponentScratchpad]] object to be inserted.
     * @return the newly created [[ComponentScratchpad]]
     */
    override def update(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      conn.sendPreparedStatement(Update, Array(
        componentScratchpad.documentId,
        componentScratchpad.version + 1,
        new DateTime,
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        componentScratchpad.version
      )).map {
        result => buildComponentScratchpad(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Delete a component scratchpad.
     *
     * @param componentScratchpad
     * @return
     */
    override def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      conn.sendPreparedStatement(Delete, Array(
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        componentScratchpad.version)
      ).map {
        result =>
          if (result.rowsAffected == 1) \/-(componentScratchpad)
          else -\/(GenericFail("The query returned no errors, but the TaskFeedback was not deleted."))
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Build a TaskFeedback object from a database result.
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildComponentScratchpad(maybeResultSet: Option[ResultSet]): \/[Fail, ComponentScratchpad] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(ComponentScratchpad(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail("Invalid data returned from the database.", exception))
      }
    }

    /**
     * Converts an optional result set into works list
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildComponentScratchpadList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[ComponentScratchpad]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(ComponentScratchpad.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a TaskScratchpad List from the rows returned.", exception))
      }
    }
  }
}
