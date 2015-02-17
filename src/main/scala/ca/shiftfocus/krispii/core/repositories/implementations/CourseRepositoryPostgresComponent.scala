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
         |  AND users.id = ?
         |  AND users.id = users_courses.user_id
         |  AND courses.id = users_courses.course_id
       """.stripMargin

    /**
     * List all courses.
     *
     * @return an  array of Courses
     */
    def list: Future[IndexedSeq[Course]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val courseList = queryResult.rows.get.map {
          item: RowData => Course(item)
        }
        courseList
      }
    }.recover {
      case exception => {
        throw exception
      }
    }

    /**
     * Return course by its project.
     *
     * @param project  the project to filter by
     *
     * @return a result set
     */
    def list(project: Project): Future[IndexedSeq[Course]] = {
      db.pool.sendPreparedStatement(ListCourseForProject, Seq[Any](project.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Course(item)
        }
      }.recover {
        case exception => {
          throw exception
        }
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
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Course]] = {
      db.pool.sendPreparedStatement((if (asTeacher) ListByTeacherId else ListCourses), Seq[Any](user.id.bytes)).map { queryResult =>
        val courseList = queryResult.rows.get.map {
          item: RowData => Course(item)
        }
        courseList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the courses associated with a user.
     */
    override def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Course]]] = {
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
        val startTimeUsC = System.nanoTime() / 1000
        val tuples = queryResult.rows.get.map { item: RowData =>
          (UUID(item("user_id").asInstanceOf[Array[Byte]]), Course(item))
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
          case Some(rowData) => Some(Course(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find student in teacher's course
     *
     * @param student
     * @param teacher
     * @return
     */
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
     * Add a user to a course
     */
    override def addUser(user: User, course: Course)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        result <- conn.sendPreparedStatement(AddUser, Array(user.id.bytes, course.id.bytes, new DateTime))
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
     * Remove a user from a course.
     */
    override def removeUser(user: User, course: Course)(implicit conn: Connection) = {
      val future = for {
        result <- conn.sendPreparedStatement(RemoveUser, Array(user.id.bytes, course.id.bytes))
      }
      yield (result.rowsAffected > 0)

      future.recover {
        case exception => {
          throw exception
        }
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
     * Add users to a course.
     *
     * @param course  the course to add users to.
     * @param users  an array of users to be added.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanCourseId = course.id.string filterNot ("-" contains _)
      val query = AddUsers + users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"('\\x$cleanCourseId', '\\x$cleanUserId', '${new DateTime}')"
      }.mkString(",")

      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Remove users from a course.
     *
     * @param course  the course to remove users from.
     * @param users  an array of the users to be removed.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanCourseId = course.id.string filterNot ("-" contains _)
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${RemoveUsers} '\\x$cleanCourseId' AND ARRAY[user_id] <@ $arrayString"""
//      Logger.debug(arrayString)
      for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)
    }


    /**
     * Remove all users from a course.
     *
     * @param course  the course to remove users from.
     * @param conn  an implicit database Connection.
     * @return a boolean indicating if the action was successful.
     */
    def removeAllUsers(course: Course)(implicit conn: Connection) = {
      for {
        result <- conn.sendPreparedStatement(RemoveAllUsers, Array[Any](course.id.bytes))
      }
      yield (result.rowsAffected > 0)
    }

    /**
     * Insert a Course row.
     *
     * @param course
     * @param conn
     * @return
     */
    def insert(course: Course)(implicit conn: Connection): Future[Course] = {
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
        result => Course(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a Course row
     *
     * @param course
     * @param conn
     * @return
     */
    def update(course: Course)(implicit conn: Connection): Future[Course] = {
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
        result => Course(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a Course row.
     *
     * @param course
     * @param conn
     * @return
     */
    def delete(course: Course)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Delete, Array(course.id.bytes, course.version))
      }
      yield { queryResult.rowsAffected > 0 }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
