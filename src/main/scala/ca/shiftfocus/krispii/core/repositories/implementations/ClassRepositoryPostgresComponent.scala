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

trait ClassRepositoryPostgresComponent extends ClassRepositoryComponent {
  self: UserRepositoryComponent with PostgresDB =>

  override val classRepository: ClassRepository = new ClassRepositoryPSQL

  private class ClassRepositoryPSQL extends ClassRepository {

    val Select =
      s"""
         |SELECT classes.id as id, classes.version as version, classes.course_id as course_id, classes.teacher_id as teacher_id,
         |       classes.name as name, classes.created_at as created_at, classes.updated_at as updated_at
       """.stripMargin

    val From =
      s"""
         |FROM classes
       """.stripMargin

    val OrderBy =
      s"""
         |ORDER BY name ASC
       """.stripMargin

    val Returning =
      s"""
         |RETURNING id, version, course_id, teacher_id, name, created_at, updated_at
       """.stripMargin

    // User CRUD operations
    val SelectAll =
      s"""
         |$Select
         |$From
         |$OrderBy
       """.stripMargin

    val SelectOne =
      s"""
         |$Select
         |$From
         |WHERE id = ?
         |$OrderBy
       """.stripMargin

    val Insert =
      s"""
         |INSERT INTO classes (id, version, course_id, teacher_id, name, created_at, updated_at)
         |VALUES (?, 1, ?, ?, ?, ?, ?)
         |$Returning
      """.stripMargin

    val Update =
      s"""
         |UPDATE classes
         |SET version = ?, course_id = ?, teacher_id = ?, name = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |$Returning
       """.stripMargin

    val Delete =
      s"""
         |DELETE FROM classes
         |WHERE id = ? AND version = ?
       """.stripMargin

    val ListByCourseId =
      s"""
         |$Select
         |$From
         |WHERE course_id = ?
         |$OrderBy
       """.stripMargin

    val ListByTeacherId =
      s"""
         |$Select
         |$From
         |WHERE teacher_id = ?
         |$OrderBy
       """.stripMargin

    val ListSections =
      s"""
         |$Select
         |$From, users_classes
         |WHERE classes.id = users_classes.class_id
         |  AND users_classes.user_id = ?
         |$OrderBy
       """.stripMargin

    val ListSectionsByTeacherId =
      s"""
         |$Select
         |$From
         |WHERE teacher_id = ?
         |$OrderBy
      """.stripMargin

    val ListSectionsForProject =
      s"""
         |$Select
         |$From, classes_projects
         |WHERE classes.id = classes_projects.class_id
         |  AND classes_projects.project_id = ?
         |$OrderBy
       """.stripMargin


    val EnablePart =
      s"""
         |INSERT INTO scheduled_classes_parts (class_id, part_id, active, created_at)
         |VALUES (?, ?, TRUE, ?)
       """.stripMargin

    val DisablePart =
      s"""
         |DELETE FROM scheduled_classes_parts
         |WHERE class_id = ?
         |  AND part_id = ?
       """.stripMargin

    val DisableForAllParts =
      s"""
         |DELETE FROM scheduled_classes_parts
         |WHERE part_id = ?
       """.stripMargin


    val AddProjects =
      s"""
         |INSERT INTO classes_projects (class_id, project_id, created_at)
         |VALUES
       """.stripMargin

    val AddUsers =
      s"""
         |INSERT INTO users_classes (class_id, user_id, created_at)
         |VALUES
       """.stripMargin

    val AddUser =
      s"""
         |INSERT INTO users_classes (user_id, class_id, created_at)
         |VALUES (?, ?, ?)
       """.stripMargin

    val RemoveUser =
      s"""
         |DELETE FROM users_classes
         |WHERE user_id = ?
         |  AND class_id = ?
       """.stripMargin

    val ListUsers =
      s"""
         |SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
         |FROM users, users_classes
         |WHERE users.id = users_classes.user_id
         |  AND users_classes.class_id = ?
         |$OrderBy
       """.stripMargin

    val ListSectionsForUserList =
      s"""
         |SELECT id, version, teacher_id, course_id, classes.name as name, classes.created_at as created_at, classes.updated_at as updated_at
         |FROM classes, users_classes
         |WHERE classes.id = users_classes.class_id
       """.stripMargin

    val RemoveUsers =
      s"""
         |DELETE FROM users_classes
         |WHERE class_id =
       """.stripMargin

    val RemoveProjects =
      s"""
         |DELETE FROM classes_projects
         |WHERE class_id =
       """.stripMargin

    val RemoveAllUsers =
      s"""
         |DELETE FROM users_classes
         |WHERE class_id = ?
       """.stripMargin

    val RemoveAllProjects =
      s"""
         |DELETE FROM classes_projects
         |WHERE class_id = ?
       """.stripMargin

    val HasProject =
      s"""
         |SELECT projects.id
         |FROM users_classes
         |INNER JOIN classes_projects ON users_classes.class_id = classes_projects.class_id
         |INNER JOIN projects ON classes_projects.project_id = projects.id
         |WHERE classes_projects.project_id = ?
         |  AND users_classes.user_id = ?
       """.stripMargin
    
    val FindUserForTeacher =
      s"""
         |SELECT users.id as id, users.version as version, users.username as username, users.email as email, 
         |       users.password_hash as password_hash, users.givenname as givenname, users.surname as surname, 
         |       users.created_at as created_at, users.updated_at as updated_at
         |FROM users, classes, users_classes
         |WHERE classes.teacher_id = ?
         |  AND users.id = ?
         |  AND users.id = users_classes.user_id
         |  AND classes.id = users_classes.class_id
       """.stripMargin

    /**
     * List all classes.
     *
     * @param conn  an implicit Connection must be in scope. This allows this
     *             method to be called inside a transaction block.
     * @return an  array of Sections
     */
    def list: Future[IndexedSeq[Class]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val classList = queryResult.rows.get.map {
          item: RowData => Class(item)
        }
        classList
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
    def list(course: Course): Future[IndexedSeq[Class]] = {
      db.pool.sendPreparedStatement(ListByCourseId, Seq[Any](course.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Class(item)
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
    def list(project: Project): Future[IndexedSeq[Class]] = {
      db.pool.sendPreparedStatement(ListSectionsForProject, Seq[Any](project.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Class(item)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Select classes based on the given user.
     *
     * @param user the user to search by
     * @param asTeacher  whether we are searching for classes this user teachers,
     *                   or classes this user is a student of.
     * @param conn  an implicit Connection must be in scope. This allows this
     *              method to be called inside a transaction block.
     * @return the found classes
     */
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Class]] = {
      db.pool.sendPreparedStatement((if (asTeacher) ListByTeacherId else ListSections), Seq[Any](user.id.bytes)).map { queryResult =>
        val classList = queryResult.rows.get.map {
          item: RowData => Class(item)
        }
        classList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the roles associated with a user.
     */
    override def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Class]]] = {
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
      val query = s"""${ListSectionsForUserList} AND ARRAY[users_classes.user_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { queryResult =>
        val startTimeUsC = System.nanoTime() / 1000
        val tuples = queryResult.rows.get.map { item: RowData =>
          (UUID(item("id").asInstanceOf[Array[Byte]]), Class(item))
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
          case Some(rowData) => Some(Class(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    override def findUserForTeacher(student: User, teacher: User): Future[Option[User]] = {
      db.pool.sendPreparedStatement(FindUserForTeacher, Array[Any](teacher.id.bytes, student.id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(User(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Add a user to a `class`
     */
    override def addUser(user: User, `class`: Class)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        result <- conn.sendPreparedStatement(AddUser, Array(user.id.bytes, `class`.id.bytes, new DateTime))
      }
      yield {
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
    override def removeUser(user: User, `class`: Class)(implicit conn: Connection) = {
      val future = for {
        result <- conn.sendPreparedStatement(RemoveUser, Array(user.id.bytes, `class`.id.bytes))
      }
      yield (result.rowsAffected > 0)

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Verify if this user has access to this project through any of his classes.
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
     * Add users to a `class`.
     *
     * @param `class`  the `class` to add users to.
     * @param users  an array of users to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addUsers(`class`: Class, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanSectionId = `class`.id.string filterNot ("-" contains _)
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
     * Add projects to a `class`.
     *
     * @param `class`  the `class` to add users to.
     * @param projects  an array of projects to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addProjects(`class`: Class, projects: IndexedSeq[Project])(implicit conn: Connection) = {
      val cleanSectionId = `class`.id.string filterNot ("-" contains _)
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
     * Remove users from a `class`.
     *
     * @param `class`  the `class` to remove users from.
     * @param users  an array of the users to be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeUsers(`class`: Class, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanSectionId = `class`.id.string filterNot ("-" contains _)
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
     * Remove projects from a `class`.
     *
     * @param `class`  the `class` to remove projects from.
     * @param projects  an array of the projectsto be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeProjects(`class`: Class, projects: IndexedSeq[Project])(implicit conn: Connection) = {
      val cleanSectionId = `class`.id.string filterNot ("-" contains _)
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
     * Remove all users from a `class`.
     *
     * @param `class`  the `class` to remove users from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllUsers(`class`: Class)(implicit conn: Connection) = {
      for {
        result <- conn.sendPreparedStatement(RemoveAllUsers, Array[Any](`class`.id.bytes))
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove all projects from a `class`.
     *
     * @param `class`  the `class` to remove projects from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllProjects(`class`: Class)(implicit conn: Connection) = {
      for {
        result <- conn.sendPreparedStatement(RemoveAllProjects, Array[Any](`class`.id.bytes))
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Insert a Section row.
     *
     * @param id the UUID of the new `class`, as a byte array
     * @param courseId the UUID of the course this `class` is attached with
     * @param maybeTeacherId the (optional) UUID of a teacher to associate this `class` with
     * @param name the name of this `class`
     * @return id of the saved/new role. Failure to insert should throw an exception.
     */
    def insert(`class`: Class)(implicit conn: Connection): Future[Class] = {
      conn.sendPreparedStatement(Insert, Array(
        `class`.id.bytes,
        `class`.courseId.bytes,
        `class`.teacherId match {
          case Some(id) => Some(id.bytes)
          case _ => None
        },
        `class`.name,
        new DateTime,
        new DateTime
      )).map {
        result => Class(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a Section row.
     *
     * @param id the UUID of the new `class`, as a byte array
     * @param version the current version of the `class` (for optimistic offline locking)
     * @param courseId the UUID of the course this `class` is attached with
     * @param maybeTeacherId the (optional) UUID of a teacher to associate this `class` with
     * @param name the name of this `class`
     * @return optional version of the updated Section, if a `class` was found to update.
     */
    def update(`class`: Class)(implicit conn: Connection): Future[Class] = {
      conn.sendPreparedStatement(Update, Array(
        (`class`.version + 1),
        `class`.courseId.bytes,
        `class`.teacherId match {
          case Some(id) => Some(id.bytes)
          case _ => None
        },
        `class`.name,
        new DateTime,
        `class`.id,
        `class`.version
      )).map {
        result => Class(result.rows.get.head)
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
    def delete(`class`: Class)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Delete, Array(`class`.id.bytes, `class`.version))
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
     * @param `class`  the `class` to enable the part for.
     * @param part  the part to enable.
     * @return a boolean indicating whether the operation was successful.
     */
    def enablePart(`class`: Class, part: Part)(implicit conn: Connection): Future[Boolean] = {
      val newStatus = for {
        result <- conn.sendPreparedStatement(EnablePart, Array(`class`.id.bytes, part.id.bytes, new DateTime))
        // We only fetch the user list so we can update the cache... the user list may be read in parallel
        // so it cannot happen in a transaction. Ensure it runs in its own connection.
        users <- userRepository.list(`class`)
      }
      yield {
        val wasEnabled = (result.rowsAffected > 0)
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
     * @param `class`  the `class` to disable the part for.
     * @param part  the part to disable.
     * @return a boolean indicating whether the operation was successful.
     */
    def disablePart(`class`: Class, part: Part)(implicit conn: Connection): Future[Boolean] = {
      val newStatus = for {
        result <- conn.sendPreparedStatement(DisablePart, Array(`class`.id.bytes, part.id.bytes))
        // We only fetch the user list so we can update the cache... the user list may be read in parallel
        // so it cannot happen in a transaction. Ensure it runs in its own connection.
        users <- userRepository.list(`class`)
      }
      yield {
        val wasDisabled = (result.rowsAffected > 0)
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
     * @param `class`  the `class` to disable the part for.
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
