package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
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
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        PostgresDB =>

  override val courseRepository: CourseRepository = new CourseRepositoryPSQL

  private class CourseRepositoryPSQL extends CourseRepository with PostgresRepository[Course] {

    def constructor(row: RowData): Course = {
      Course(
        UUID(row("id").asInstanceOf[Array[Byte]]),
        row("version").asInstanceOf[Long],
        UUID(row("teacher_id").asInstanceOf[Array[Byte]]),
        row("name").asInstanceOf[String],
        new Color(Option(row("color").asInstanceOf[Int]).getOrElse(0)),
        None,
        row("created_at").asInstanceOf[DateTime],
        row("updated_at").asInstanceOf[DateTime]
      )
    }

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
//    val ListCoursesByTeacherId =
//      s"""
//         |$Select
//         |$From
//         |WHERE teacher_id = ?
//         |$OrderBy
//      """.stripMargin

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
//    val ListUsers =
//      s"""
//         |SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
//         |FROM users, users_courses
//         |WHERE users.id = users_courses.user_id
//         |  AND users_courses.course_id = ?
//         |$OrderBy
//       """.stripMargin

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
//    val RemoveProjects =
//      s"""
//         |DELETE FROM courses_projects
//         |WHERE course_id =
//       """.stripMargin

    val RemoveAllUsers =
      s"""
         |DELETE FROM users_courses
         |WHERE course_id = ?
       """.stripMargin

    // TODO - not used
//    val RemoveAllProjects =
//      s"""
//         |DELETE FROM courses_projects
//         |WHERE course_id = ?
//       """.stripMargin


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
         |  AND users_courses.user_id = ?
         |  AND courses.id = users_courses.course_id
       """.stripMargin

    /**
     * List all courses.
     *
     * @return an  array of Courses
     */
    def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Course]]] = {
      queryList(SelectAll)
    }

    /**
     * Return course by its project.
     *
     * @param project  the project to filter by
     *
     * @return a result set
     */
    def list(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Course]]] = {
      queryList(ListCourseForProject, Seq[Any](project.id.bytes))
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
    def list(user: User, asTeacher: Boolean = false)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Course]]] = {
      queryList(if (asTeacher) ListByTeacherId else ListCourses, Seq[Any](user.id.bytes))
    }

    /**
     * List the courses associated with a user.
     */
    override def list(users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Map[UUID, IndexedSeq[Course]]]] = {
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
                (UUID(item("user_id").asInstanceOf[Array[Byte]]), constructor(item))
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
          case exception: NoSuchElementException => throw exception
        }
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Course]] = {
      queryOne(SelectOne, Array[Any](id.bytes))
    }

    /**
     * Find student in teacher's course
     *
     * @param student
     * @param teacher
     * @return
     */
    override def findUserForTeacher(student: User, teacher: User)(implicit conn: Connection): Future[\/[Fail, User]] = {
      conn.sendPreparedStatement(FindUserForTeacher, Array[Any](teacher.id.bytes, student.id.bytes)).flatMap { result =>
        result.rows match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => userRepository.find(UUID(firstRow("user_id").asInstanceOf[Array[Byte]]))
            case None => Future successful -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => Future successful -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Add a user to a course
     */
    override def addUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Unit]] = {
      queryNumRows(AddUser, Array(user.id.bytes, course.id.bytes, new DateTime))(1 == _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(NoResults("The query succeeded but the course could not be added."))
        case -\/(error) => -\/(error)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_courses_pkey") -\/(UniqueFieldConflict(s"User has already this course"))
            else if (nField == "users_courses_user_id_fkey") -\/(ReferenceConflict(s"Referenced user with id ${user.id.string} doesn't exist"))
            else if (nField == "users_courses_course_id_fkey") -\/(ReferenceConflict(s"Referenced course with id ${course.id.string} doesn't exist"))
            else  throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Remove a user from a course.
     */
    override def removeUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Unit]] = {
      queryNumRows(RemoveUser, Array(user.id.bytes, course.id.bytes))(1 == _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(NoResults("The query succeeded but the course could not be added."))
        case -\/(error) => -\/(error)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_courses_pkey") -\/(UniqueFieldConflict(s"User has already this course"))
            else if (nField == "users_courses_user_id_fkey") -\/(ReferenceConflict(s"Referenced user with id ${user.id.string} doesn't exist"))
            else if (nField == "users_courses_course_id_fkey") -\/(ReferenceConflict(s"Referenced course with id ${course.id.string} doesn't exist"))
            else  throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
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
      conn.sendPreparedStatement(HasProject, Array[Any](project.id.bytes, user.id.bytes)).map { result =>
        if (result.rows.get.length > 0) {
          \/-(true)
        } else {
          \/-(false)
        }
      }.recover {
        case exception: Throwable => throw exception
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
    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Unit]] = {
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
          \/-( () )
        }
      wasAdded.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_courses_pkey") -\/(UniqueFieldConflict(s"User is already in the class"))
            else if (nField == "users_courses_user_id_fkey") -\/(ReferenceConflict(s"User doesn't exist"))
            else if (nField == "users_courses_course_id_fkey") -\/(ReferenceConflict(s"Class doesn't exist"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
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
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Unit]] = {
      val cleanCourseId = course.id.string filterNot ("-" contains _)
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${RemoveUsers} '\\x$cleanCourseId' AND ARRAY[user_id] <@ $arrayString"""

      val wasRemoved = for {
        result <- conn.sendQuery(query)
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-( () )
        }

      wasRemoved.recover {
        case exception: Throwable => throw exception
      }
    }


    /**
     * Remove all users from a course.
     *
     * @param course  the course to remove users from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllUsers(course: Course)(implicit conn: Connection): Future[\/[Fail, Unit]] = {
      queryNumRows(RemoveAllUsers, Seq[Any](course.id.bytes))(1 <= _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(GenericFail("No rows were affected"))
        case -\/(error) => -\/(error)
      }

      val wasRemoved = for {
        result <- conn.sendPreparedStatement(RemoveAllUsers, Array[Any](course.id.bytes))
      }
      yield
        if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-( () )
        }

      wasRemoved.recover {
        case exception: Throwable => throw exception
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
      val params = Seq[Any](
        course.id.bytes, course.teacherId.bytes, course.name,
        course.color.getRGB, new DateTime, new DateTime
      )

      queryOne(Insert, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "course_pkey") -\/(UniqueFieldConflict(s"Class already exists"))
            else if (nField == "courses_teacher_id_fkey") -\/(ReferenceConflict(s"Teacher doesn't exist"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
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
      val params = Seq[Any](
        course.version + 1, course.teacherId.bytes, course.name,
        course.color.getRGB, new DateTime, course.id.bytes, course.version
      )
      queryOne(Update, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "courses_teacher_id_fkey") -\/(ReferenceConflict(s"Tried to reference a teacher that doesn't exist"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
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
      queryOne(Delete, Array(course.id.bytes, course.version)).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            // TODO check nField name
            if (nField == "projects_courses_course_id_fkey") -\/(UniqueFieldConflict(s"Can't delete a course when it still has projects."))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }
  }
}
