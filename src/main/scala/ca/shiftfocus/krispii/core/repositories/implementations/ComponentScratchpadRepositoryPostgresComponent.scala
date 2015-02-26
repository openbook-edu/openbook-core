package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
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

  private class ComponentScratchpadRepositoryPSQL extends ComponentScratchpadRepository with PostgresRepository[ComponentScratchpad] {

    override def constructor(row: RowData): ComponentScratchpad = {
      ComponentScratchpad(
        UUID(row("user_id").asInstanceOf[Array[Byte]]),
        UUID(row("component_id").asInstanceOf[Array[Byte]]),
        row("version").asInstanceOf[Long],
        row("document_id").asInstanceOf[UUID],
        row("created_at").asInstanceOf[DateTime],
        row("updated_at").asInstanceOf[DateTime]
      )
    }

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
      RETURNING $Fields
    """
    
    /**
     * Select rows by their user ID.
     *
     * @param user the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      queryList(SelectAllForUser, Array[Any](user.id.bytes))
    }

    /**
     * Select rows by their task ID.
     *
     * @param component the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def list(component: Component)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      queryList(SelectAllForComponent, Array[Any](component.id.bytes))
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(user: User, component: Component)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      queryOne(SelectOne, Array[Any](user.id.bytes, component.id.bytes))
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
      queryOne(Insert, Seq(
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        1L,
        new DateTime,
        new DateTime,
        componentScratchpad.documentId
      ))
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
      queryOne(Update, Seq(
        componentScratchpad.documentId,
        componentScratchpad.version + 1,
        new DateTime,
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        componentScratchpad.version
      ))
    }

    /**
     * Delete a component scratchpad.
     *
     * @param componentScratchpad
     * @return
     */
    override def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]] = {
      queryOne(Delete, Seq(
        componentScratchpad.userId.bytes,
        componentScratchpad.componentId.bytes,
        componentScratchpad.version)
      )
    }
  }
}
