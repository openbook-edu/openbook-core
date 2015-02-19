package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.fail._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}
import scalaz.syntax.either._

trait CourseRepositoryPostgresComponent extends CourseRepositoryComponent {
  self: UserRepositoryComponent with PostgresDB =>

  override val courseRepository: CourseRepository = new CourseRepositoryPSQL

  private class CourseRepositoryPSQL extends CourseRepository {

    val Select =
      s"""
         |SELECT courses.id as id, courses.version as version, courses.teacher_id as teacher_id,
         |       courses.name as name, courses.color as color, courses.created_at as created_at, courses.updated_at as updated_at
       """.stripMargin

    val From =
      s"""
         |FROM courses
       """.stripMargin

    val OrderBy =
      s"""
         |ORDER BY name ASC
       """.stripMargin

    val Returning =
      s"""
         |RETURNING id, version, teacher_id, name, color, created_at, updated_at
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
         |INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
         |VALUES (?, 1, ?, ?, ?, ?, ?)
         |$Returning
      """.stripMargin

    val Update =
      s"""
         |UPDATE courses
         |SET version = ?, teacher_id = ?, name = ?, color = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |$Returning
       """.stripMargin

    val Delete =
      s"""
         |DELETE FROM courses
         |WHERE id = ? AND version = ?
       """.stripMargin

    val ListByTeacherId =
      s"""
         |$Select
         |$From
         |WHERE teacher_id = ?
         |$OrderBy
       """.stripMargin

    val ListCourses =
      s"""
         |$Select
         |$From, users_courses
         |WHERE courses.id = users_courses.course_id
         |  AND users_courses.user_id = ?
         |$OrderBy
       """.stripMargin

    // TODO - not used
    val ListCoursesByTeacherId =
      s"""
         |$Select
         |$From
         |WHERE teacher_id = ?
         |$OrderBy
      """.stripMargin

    val ListCourseForProject =
      s"""
         |$Select
         |$From, projects
         |WHERE courses.id = projects.course_id
         |  AND projects.id = ?
         |$OrderBy
       """.stripMargin

    val AddUsers =
      s"""
         |INSERT INTO users_courses (course_id, user_id, created_at)
         |VALUES
       """.stripMargin

    val AddUser =
      s"""
         |INSERT INTO users_courses (user_id, course_id, created_at)
         |VALUES (?, ?, ?)
       """.stripMargin

    val RemoveUser =
      s"""
         |DELETE FROM users_courses
         |WHERE user_id = ?
         |  AND course_id = ?
       """.stripMargin

    // TODO - not used, is implemented in UserRepo
    val ListUsers =
      s"""
         |SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
         |FROM users, users_courses
         |WHERE users.id = users_courses.user_id
         |  AND users_courses.course_id = ?
         |$OrderBy
       """.stripMargin

    val ListCoursesForUserList =
      s"""
         |SELECT user_id, id, version, teacher_id, color, courses.name as name, courses.created_at as created_at, courses.updated_at as updated_at
         |FROM courses, users_courses
         |WHERE courses.id = users_courses.course_id
       """.stripMargin

    val RemoveUsers =
      s"""
         |DELETE FROM users_courses
         |WHERE course_id =
       """.stripMargin

    // TODO - not used
    val RemoveProjects =
      s"""
         |DELETE FROM courses_projects
         |WHERE course_id =
       """.stripMargin

    val RemoveAllUsers =
      s"""
         |DELETE FROM users_courses
         |WHERE course_id = ?
       """.stripMargin

    // TODO - not used
    val RemoveAllProjects =
      s"""
         |DELETE FROM courses_projects
         |WHERE course_id = ?
       """.stripMargin


    val HasProject =
      s"""
         |SELECT projects.id
         |FROM projects
         |INNER JOIN courses ON courses.id = projects.course_id
         |INNER JOIN users_courses ON users_courses.course_id = courses.id
         |WHERE projects.id = ?
         |  AND users_courses.user_id = ?
       """.stripMargin

    val FindUserForTeacher =
      s"""
         |SELECT users.id as id, users.version as version, users.username as username, users.email as email,
         |       users.password_hash as password_hash, users.givenname as givenname, users.surname as surname,
         |       users.created_at as created_at, users.updated_at as updated_at
         |FROM users, courses, users_courses
         |WHERE courses.teacher_id = ?
         |  AND users.id = ?
         |  AND users.id = users_courses.user_id
         |  AND courses.id = users_courses.course_id
       """.stripMargin

    /**
     * List all courses.
     *
     * @return an  array of Courses
     */
    def list: Future[\/[Fail, IndexedSeq[Course]]] = {
      db.pool.sendQuery(SelectAll).map {
        result => buildCourseList(result.rows)
      }
    }.recover {
      case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
    }

    /**
     * Return class by its project.
     *
     * @param project  the project to filter by
     *
     * @return a result set
     */
    def list(project: Project): Future[\/[Fail, IndexedSeq[Course]]] = {
      db.pool.sendPreparedStatement(ListCourseForProject, Seq[Any](project.id.bytes)).map {
        result => buildCourseList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Select courses based on the given user.
     *
     * @param user the user to search by
     * @param asTeacher  whether we are searching for courses this user teachers,
     *                   or courses this user is a student of.
     *
     * @return the found courses
     */
    def list(user: User, asTeacher: Boolean = false): Future[\/[Fail, IndexedSeq[Course]]] = {
      db.pool.sendPreparedStatement((if (asTeacher) ListByTeacherId else ListCourses), Seq[Any](user.id.bytes)).map {
        result => buildCourseList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * List the courses associated with a user.
     */
    override def list(users: IndexedSeq[User]): Future[\/[Fail, Map[UUID, IndexedSeq[Course]]]] = {
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
      val query = s"""${ListCoursesForUserList} AND ARRAY[users_courses.user_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { queryResult =>
        try {
          queryResult.rows match {
            case Some(resultSet) => {
              val startTimeUsC = System.nanoTime() / 1000
              val tuples = queryResult.rows.get.map { item: RowData =>
                (UUID(item("user_id").asInstanceOf[Array[Byte]]), Course(item))
              }
              val tupledWithUsers = users.map { user =>
                (user.id, tuples.filter(_._1 == user.id).map(_._2))
              }
              \/-(tupledWithUsers.toMap)
            }
            case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
          }
        }
        catch {
          case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build Role(s) from the row returned.", exception))
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(id: UUID): Future[\/[Fail, Course]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildCourse(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Find student in teacher's class
     *
     * @param student
     * @param teacher
     * @return
     */
    override def findUserForTeacher(student: User, teacher: User): Future[\/[Fail, User]] = {
      db.pool.sendPreparedStatement(FindUserForTeacher, Array[Any](teacher.id.bytes, student.id.bytes)).map { result =>
        try {
          result.rows match {
            case Some(resultSet) => resultSet.headOption match {
              case Some(firstRow) => \/-(User(firstRow))
              case None => -\/(NoResults("The query was successful but ResultSet was empty."))
            }
            case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
          }
        }
        catch {
          case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a user from the row returned.", exception))
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Add a user to a course
     */
    override def addUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val future = for {
        result <- conn.sendPreparedStatement(AddUser, Array(user.id.bytes, course.id.bytes, new DateTime))
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }
      future.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Remove a user from a class.
     */
    override def removeUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val future = for {
        result <- conn.sendPreparedStatement(RemoveUser, Array(user.id.bytes, course.id.bytes))
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }
      future.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Verify if this user has access to this project through any of his courses.
     *
     * @param user
     * @param project
     * @param conn
     * @return
     */
    override def hasProject(user: User, project: Project)(implicit conn: Connection): Future[\/[Fail, Boolean]] = {
      val future = for {
        result <- conn.sendPreparedStatement(HasProject, Array[Any](project.id.bytes, user.id.bytes))
      }
      yield
        if (result.rows.get.length > 0) {
          \/-(true)
        } else {
          \/-(false)
        }
      future.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Add users to a course.
     *
     * @param course  the course to add users to.
     * @param users  an array of users to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val cleanCourseId = course.id.string filterNot ("-" contains _)
      val query = AddUsers + users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"('\\x$cleanCourseId', '\\x$cleanUserId', '${new DateTime}')"
      }.mkString(",")

      val wasAdded = for {
        result <- conn.sendQuery(query)
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }
      wasAdded.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_courses_pkey") -\/(EntityAlreadyExists(s"User is already in the class"))
            else if (nField == "users_courses_user_id_fkey") -\/(EntityReferenceFieldError(s"User doesn't exist"))
            else if (nField == "users_courses_course_id_fkey") -\/(EntityReferenceFieldError(s"Class doesn't exist"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove users from a course.
     *
     * @param course  the course to remove users from.
     * @param users  an array of the users to be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val cleanCourseId = course.id.string filterNot ("-" contains _)
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${RemoveUsers} '\\x$cleanCourseId' AND ARRAY[user_id] <@ $arrayString"""
      Logger.debug(arrayString)

      val wasRemoved = for {
        result <- conn.sendQuery(query)
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }

      wasRemoved.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }


    /**
     * Remove all users from a course.
     *
     * @param course  the course to remove users from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllUsers(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val wasRemoved = for {
        result <- conn.sendPreparedStatement(RemoveAllUsers, Array[Any](course.id.bytes))
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }

      wasRemoved.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Insert a Course row.
     *
     * @param course
     * @param conn
     * @return
     */
    def insert(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      conn.sendPreparedStatement(Insert, Array(
        course.id.bytes,
        course.teacherId match {
          case Some(id) => Some(id.bytes)
          case _ => None
        },
        course.name,
        course.color.getRGB,
        new DateTime,
        new DateTime
      )).map {
        result => buildCourse(result.rows)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "course_pkey") -\/(EntityAlreadyExists(s"Class already exists"))
            else if (nField == "courses_teacher_id_fkey") -\/(EntityReferenceFieldError(s"Teacher doesn't exist"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update a Course row
     *
     * @param course
     * @param conn
     * @return
     */
    def update(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      conn.sendPreparedStatement(Update, Array(
        (course.version + 1),
        course.teacherId match {
          case Some(id) => Some(id.bytes)
          case _ => None
        },
        course.name,
        course.color.getRGB,
        new DateTime,
        course.id.bytes,
        course.version
      )).map {
        result => buildCourse(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Delete a Course row.
     *
     * @param course
     * @param conn
     * @return
     */
    def delete(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Delete, Array(course.id.bytes, course.version))
      }
      yield
        if (queryResult.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(course)
        }

      future.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            // TODO check nField name
            if (nField == "projects_course_id_fkey") -\/(EntityAlreadyExists(s"Class has referece in projects table"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Transform result rows into a single course.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildCourse(maybeResultSet: Option[ResultSet]): \/[Fail, Course] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(Course(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Course from the row returned.", exception))
      }
    }

    /**
     * Converts an optional result set into courses list
     *
     * @param maybeResultSet
     * @return
     */
    private def buildCourseList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[Course]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(Course.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Course List from the rows returned.", exception))
      }
    }
  }
}
