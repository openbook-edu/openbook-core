package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.common.models._
import play.api.Play.current
import play.api.cache.Cache
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.common.services.datasource.PostgresDB

trait SectionRepositoryPostgresComponent extends SectionRepositoryComponent {
  self: UserRepositoryComponent with PostgresDB =>

  override val sectionRepository: SectionRepository = new SectionRepositoryPSQL

  private class SectionRepositoryPSQL extends SectionRepository {
    def fields = Seq("course_id", "teacher_id", "name")
    def table = "sections"
    def orderBy = "name ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
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

    val Restore = s"""
      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectOneByCourseId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE course_id = ?
        AND status = 1
      ORDER BY name ASC
    """

    val SelectByTeacherId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE teacher_id = ?
        AND status = 1
      ORDER BY name ASC
    """

    val ListSections = s"""
      SELECT id, version, teacher_id, course_id, sections.name as name, sections.created_at as created_at, sections.updated_at as updated_at
      FROM sections, users_sections
      WHERE sections.id = users_sections.section_id
        AND users_sections.user_id = ?
        AND sections.status = 1
      ORDER BY name ASC
    """

    val ListSectionsByTeacherId = s"""
      SELECT id, version, teacher_id, course_id, sections.name as name, sections.created_at as created_at, sections.updated_at as updated_at
      FROM sections
      WHERE teacher_id = ?
        AND sections.status = 1
      ORDER BY name ASC
    """

    val ListSectionsForProject = s"""
      SELECT id, version, teacher_id, course_id, sections.name as name, sections.created_at as created_at, sections.updated_at as updated_at
      FROM sections, sections_projects
      WHERE sections.id = sections_projects.section_id
        AND sections_projects.project_id = ?
        AND sections.status = 1
      ORDER BY name ASC
    """

    val EnablePart = s"""
      INSERT INTO scheduled_sections_parts (section_id, part_id, active, created_at)
      VALUES (?, ?, TRUE, ?)
    """

    val DisablePart = s"""
      DELETE FROM scheduled_sections_parts
      WHERE section_id = ?
        AND part_id = ?
    """

    val DisableForAllParts =
      s"""
         |DELETE FROM scheduled_sections_parts
         |WHERE part_id = ?
       """.stripMargin

    val AddProjects = s"""
      INSERT INTO sections_projects (section_id, project_id, created_at)
      VALUES
    """

    val AddUsers = s"""
      INSERT INTO users_sections (section_id, user_id, created_at)
      VALUES
    """

    val AddUser = """
      INSERT INTO users_sections (user_id, section_id, created_at)
      VALUES (?, ?, ?)
    """

    val RemoveUser = """
      DELETE FROM users_sections
      WHERE user_id = ?
        AND section_id = ?
    """

    val ListUsers = s"""
      SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, users_sections
      WHERE users.id = users_sections.user_id
        AND users_sections.section_id = ?
        AND users.status = 1
      ORDER BY $orderBy
    """

    val ListSectionsForUserList = """
      SELECT id, version, teacher_id, course_id, sections.name as name, sections.created_at as created_at, sections.updated_at as updated_at
      FROM sections, users_sections
      WHERE sections.id = users_sections.section_id
        AND sections.status = 1
    """

    val RemoveUsers = s"""
      DELETE FROM users_sections
      WHERE section_id =
    """

    val RemoveProjects = s"""
      DELETE FROM sections_projects
      WHERE section_id =
    """

    val RemoveAllUsers = s"""
      DELETE FROM users_sections
      WHERE section_id = ?
    """

    val RemoveAllProjects = s"""
      DELETE FROM sections_projects
      WHERE section_id = ?
    """

    val HasProject = s"""
      SELECT projects.id
      FROM users_sections
      INNER JOIN sections_projects ON users_sections.section_id = sections_projects.section_id
      INNER JOIN projects ON sections_projects.project_id = projects.id
      WHERE sections_projects.project_id = ?
        AND users_sections.user_id = ?
        AND projects.status = 1
    """

    /**
     * Cache a section into the in-memory cache.
     *
     * @param section the [[Section]] to be cached
     * @return the [[Section]] that was cached
     */
    private def cache(section: Section): Section = {
      Cache.set(s"sections[${section.id}]", section, db.cacheExpiry)
      section
    }

    /**
     * Remove a section from the in-memory cache.
     *
     * @param section the [[Section]] to be uncached
     * @return the [[Section]] that was uncached
     */
    private def uncache(section: Section): Section = {
      Cache.remove(s"sections[${section.id}]")
      section
    }

    /**
     * List all sections.
     *
     * @param conn  an implicit Connection must be in scope. This allows this
     *             method to be called inside a transaction block.
     * @return an  array of Sections
     */
    def list: Future[IndexedSeq[Section]] = {
      Logger.trace("SectionRepositoryPSQL.list")
      Cache.getAs[IndexedSeq[UUID]]("sections.list") match {
        case Some(sectionIdList) => {
          Future.sequence(sectionIdList.map { sectionId =>
            find(sectionId).map(_.get)
          })
        }
        case None => {
          db.pool.sendQuery(SelectAll).map { queryResult =>
            val sectionList = queryResult.rows.get.map {
              item: RowData => cache(Section(item))
            }
            Cache.set(s"sections.list", sectionList.map(_.id), db.cacheExpiry)
            sectionList
          }
        }
      }
    }.recover {
      case exception => {
        throw exception
      }
    }

    /**
     * Select rows by their course ID.
     *
     * @param course  the course to filter by
     * @param conn  an implicit Connection must be in scope. This allows this
     *              method to be called inside a transaction block.
     * @return an array of Sections
     */
    def list(course: Course): Future[IndexedSeq[Section]] = {
      db.pool.sendPreparedStatement(SelectOneByCourseId, Seq[Any](course.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Section(item)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Select rows by their course ID.
     *
     * @param project  the project to filter by
     * @param conn  an implicit Connection must be in scope. This allows this
     *              method to be called inside a transaction block.
     * @return a result set
     */
    def list(project: Project): Future[IndexedSeq[Section]] = {
      db.pool.sendPreparedStatement(ListSectionsForProject, Seq[Any](project.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Section(item)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Select sections based on the given user.
     *
     * @param user the user to search by
     * @param asTeacher  whether we are searching for sections this user teachers,
     *                   or sections this user is a student of.
     * @param conn  an implicit Connection must be in scope. This allows this
     *              method to be called inside a transaction block.
     * @return the found sections
     */
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Section]] = {
      // We cache a different list depending on whether we're searching for this user's sections
      // they are a student of, or the ones they are a teacher for.
      val cacheString = if (asTeacher) s"section_ids:teacher:${user.id.string}"
                        else s"section_ids:user:${user.id.string}"
      // Attempt to load the list of sections from cache
      Cache.getAs[IndexedSeq[UUID]](cacheString) match {
        case Some(sectionIdList) => {
          // If we found the list in cache, load each section individually
          val fSectionOptionList = Future.sequence(sectionIdList.map(find))
          fSectionOptionList.map { sectionOptionList =>
            sectionOptionList.map(_.get)
          }
        }
        case None => {
          // Otherwise, submit a database query and cache the list once we have it
          db.pool.sendPreparedStatement((if (asTeacher) SelectByTeacherId else ListSections), Seq[Any](user.id.bytes)).map { queryResult =>
            val sectionList = queryResult.rows.get.map {
              item: RowData => cache(Section(item))
            }
            Cache.set(cacheString, sectionList.map(_.id))
            sectionList
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
      }
    }

    /**
     * List the roles associated with a user.
     */
    override def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Section]]] = {
      val arrayString = users.map { user =>
        val userId = user.id.string
        val cleanUserId =
          userId.slice(0, 8) +
          userId.slice(9, 13) +
          userId.slice(14, 18) +
          userId.slice(19, 23) +
          userId.slice(24, 36)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${ListSectionsForUserList} AND ARRAY[users_sections.user_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { queryResult =>
        val startTimeUsC = System.nanoTime() / 1000
        val tuples = queryResult.rows.get.map { item: RowData =>
          (UUID(item("id").asInstanceOf[Array[Byte]]), Section(item))
        }
        val tupledWithUsers = users.map { user =>
          (user.id, tuples.filter(_._1 == user.id).map(_._2))
        }
        tupledWithUsers.toMap
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(id: UUID) = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Section(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Add a user to a section
     */
    override def addUser(user: User, section: Section)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        result <- conn.sendPreparedStatement(AddUser, Array(user.id.bytes, section.id.bytes, new DateTime))
      }
      yield {
        Cache.remove(s"users.list.section[${section.id.string}]")
        result.rowsAffected > 0
      }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Remove a role from a user.
     */
    override def removeUser(user: User, section: Section)(implicit conn: Connection) = {
      val future = for {
        result <- conn.sendPreparedStatement(RemoveUser, Array(user.id.bytes, section.id.bytes))
      }
      yield (result.rowsAffected > 0)

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Verify if this user has access to this project through any of his sections.
     * @type {[type]}
     */
    override def hasProject(user: User, project: Project)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        result <- conn.sendPreparedStatement(HasProject, Array[Any](project.id.bytes, user.id.bytes))
      }
      yield (result.rows.get.length > 0)

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Add users to a section.
     *
     * @param section  the section to add users to.
     * @param users  an array of users to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addUsers(section: Section, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanSectionId = section.id.string filterNot ("-" contains _)
      val query = AddUsers + users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"('\\x$cleanSectionId', '\\x$cleanUserId', '${new DateTime}')"
      }.mkString(",")

      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Add projects to a section.
     *
     * @param section  the section to add users to.
     * @param projects  an array of projects to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addProjects(section: Section, projects: IndexedSeq[Project])(implicit conn: Connection) = {
      val cleanSectionId = section.id.string filterNot ("-" contains _)
      val query = AddProjects + projects.map { project =>
        val cleanProjectId = project.id.string filterNot ("-" contains _)
        s"('\\x$cleanSectionId', '\\x$cleanProjectId', '${new DateTime}')"
      }.mkString(",")

      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove users from a section.
     *
     * @param section  the section to remove users from.
     * @param users  an array of the users to be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeUsers(section: Section, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanSectionId = section.id.string filterNot ("-" contains _)
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${RemoveUsers} '\\x$cleanSectionId' AND ARRAY[user_id] <@ $arrayString"""
      Logger.debug(arrayString)
      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove projects from a section.
     *
     * @param section  the section to remove projects from.
     * @param projects  an array of the projectsto be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeProjects(section: Section, projects: IndexedSeq[Project])(implicit conn: Connection) = {
      val cleanSectionId = section.id.string filterNot ("-" contains _)
      val arrayString = projects.map { project =>
        val cleanProjectId = project.id.string filterNot ("-" contains _)
        s"decode('$cleanProjectId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${RemoveProjects} '\\x$cleanSectionId' AND ARRAY[project_id] <@ $arrayString"""
      Logger.debug(arrayString)
      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove all users from a section.
     *
     * @param section  the section to remove users from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllUsers(section: Section)(implicit conn: Connection) = {
      for {
        result <- conn.sendPreparedStatement(RemoveAllUsers, Array[Any](section.id.bytes))
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove all projects from a section.
     *
     * @param section  the section to remove projects from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllProjects(section: Section)(implicit conn: Connection) = {
      for {
        result <- conn.sendPreparedStatement(RemoveAllProjects, Array[Any](section.id.bytes))
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Insert a Section row.
     *
     * @param id the UUID of the new section, as a byte array
     * @param courseId the UUID of the course this section is attached with
     * @param maybeTeacherId the (optional) UUID of a teacher to associate this section with
     * @param name the name of this section
     * @return id of the saved/new role. Failure to insert should throw an exception.
     */
    def insert(section: Section)(implicit conn: Connection): Future[Section] = {
      conn.sendPreparedStatement(Insert, Array(
        section.id.bytes,
        new DateTime,
        new DateTime,
        section.courseId,
        section.teacherId,
        section.name
      )).map {
        result => Section(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a Section row.
     *
     * @param id the UUID of the new section, as a byte array
     * @param version the current version of the section (for optimistic offline locking)
     * @param courseId the UUID of the course this section is attached with
     * @param maybeTeacherId the (optional) UUID of a teacher to associate this section with
     * @param name the name of this section
     * @return optional version of the updated Section, if a section was found to update.
     */
    def update(section: Section)(implicit conn: Connection): Future[Section] = {
      conn.sendPreparedStatement(Update, Array(
        section.courseId,
        section.teacherId,
        section.name,
        (section.version + 1),
        new DateTime,
        section.id,
        section.version
      )).map {
        result => Section(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     *
     * @param id
     * @return
     */
    def delete(section: Section)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(section.id.bytes, section.version))
      }
      yield { queryResult.rowsAffected > 0 }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Enable a particular project part for this Section's users.
     *
     * @param section  the section to enable the part for.
     * @param part  the part to enable.
     * @return a boolean indicating whether the operation was successful.
     */
    def enablePart(section: Section, part: Part)(implicit conn: Connection): Future[Boolean] = {
      val newStatus = for {
        result <- conn.sendPreparedStatement(EnablePart, Array(section.id.bytes, part.id.bytes, new DateTime))
        // We only fetch the user list so we can update the cache... the user list may be read in parallel
        // so it cannot happen in a transaction. Ensure it runs in its own connection.
        users <- userRepository.list(section)
      }
      yield {
        val wasEnabled = (result.rowsAffected > 0)
        if (wasEnabled) {
          // When a part is enabled or disabled, update the cached values!
          Cache.set(s"part.is_enabled.part[${part.id.string}].section[${section.id.string}]", true, db.cacheExpiry)
          users.map { user =>
            Cache.set(s"part.is_enabled.part[${part.id.string}].user[${user.id.string}]", true, db.cacheExpiry)
          }
        }
        wasEnabled
      }
      newStatus.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Disable a particular project part for this Section's users.
     *
     * @param section  the section to disable the part for.
     * @param part  the part to disable.
     * @return a boolean indicating whether the operation was successful.
     */
    def disablePart(section: Section, part: Part)(implicit conn: Connection): Future[Boolean] = {
      val newStatus = for {
        result <- conn.sendPreparedStatement(DisablePart, Array(section.id.bytes, part.id.bytes))
        // We only fetch the user list so we can update the cache... the user list may be read in parallel
        // so it cannot happen in a transaction. Ensure it runs in its own connection.
        users <- userRepository.list(section)
      }
      yield {
        val wasDisabled = (result.rowsAffected > 0)
        if (wasDisabled) {
          // When a part is enabled or disabled, update the cached values!
          Cache.set(s"part.is_enabled.part[${part.id.string}].section[${section.id.string}]", false, db.cacheExpiry)
          users.map { user =>
            Cache.set(s"part.is_enabled.part[${part.id.string}].user[${user.id.string}]", false, db.cacheExpiry)
          }
        }
        wasDisabled
      }
      newStatus.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Disable a particular project part for this Section's users.
     *
     * @param section  the section to disable the part for.
     * @param part  the part to disable.
     * @return a boolean indicating whether the operation was successful.
     */
    def disablePart(part: Part)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(DisableForAllParts, Array(part.id.bytes)).map {
        result => { result.rowsAffected > 0 }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
