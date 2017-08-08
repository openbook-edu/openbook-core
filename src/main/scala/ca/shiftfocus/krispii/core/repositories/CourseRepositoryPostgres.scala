package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ ResultSet, RowData, Connection }
import java.awt.Color

import play.api.Logger

// scalastyle:ignore
import java.util.NoSuchElementException
import java.util.UUID
import org.joda.time.DateTime
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ \/, -\/, \/- }
import scalaz.syntax.either._

class CourseRepositoryPostgres(val userRepository: UserRepository) extends CourseRepository with PostgresRepository[Course] {

  override val entityName = "Course"

  def constructor(row: RowData): Course = {
    Course(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("teacher_id").asInstanceOf[UUID],
      row("name").asInstanceOf[String],
      new Color(Option(row("color").asInstanceOf[Int]).getOrElse(0)),
      row("slug").asInstanceOf[String],
      row("enabled").asInstanceOf[Boolean],
      row("archived").asInstanceOf[Boolean],
      row("is_deleted").asInstanceOf[Boolean],
      row("chat_enabled").asInstanceOf[Boolean],
      row("scheduling_enabled").asInstanceOf[Boolean],
      None,
      row("theater_mode").asInstanceOf[Boolean],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "courses"
  val Fields = "id, version, teacher_id, name, color, slug, enabled, archived, is_deleted, chat_enabled, scheduling_enabled, theater_mode, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.name ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
       |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       |LIMIT 1
     """.stripMargin

  val SelectOneBySlug =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
       |LIMIT 1
     """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, get_slug(?, '$Table', ?), ?, false, false, ?, ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin
  }

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, teacher_id = ?, name = ?, color = ?, slug = get_slug(?, '$Table', ?), enabled = ?,
       |    archived = ?, scheduling_enabled = ?, theater_mode = ?, chat_enabled = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |UPDATE $Table
       |SET is_deleted = true
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val ListByTeacherId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE teacher_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListCourses =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, users_courses
       |WHERE $Table.id = users_courses.course_id
       |  AND users_courses.user_id = ?
       |ORDER BY $OrderBy
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
       |SELECT $FieldsWithTable
       |FROM $Table, projects
       |WHERE $Table.id = projects.course_id
       |  AND projects.id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val AddUsers =
    """
       |INSERT INTO users_courses (course_id, user_id, created_at)
       |VALUES
     """.stripMargin

  val AddUser =
    """
       |INSERT INTO users_courses (user_id, course_id, created_at)
       |VALUES (?, ?, ?)
     """.stripMargin

  val RemoveUser =
    """
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
       |SELECT user_id, $FieldsWithTable
       |FROM $Table, users_courses
       |WHERE $Table.id = users_courses.course_id
       | AND ARRAY[users_courses.user_id] <@ ?
     """.stripMargin

  val RemoveUsers =
    """
       |DELETE FROM users_courses
       |WHERE course_id = ?
       |  AND ARRAY[user_id] <@ ?
     """.stripMargin

  val RemoveAllUsers =
    """
       |DELETE FROM users_courses
       |WHERE course_id = ?
     """.stripMargin

  val HasProject =
    s"""
       |SELECT projects.id
       |FROM projects
       |INNER JOIN $Table ON $Table.id = projects.course_id
       |INNER JOIN users_courses ON users_courses.course_id = $Table.id
       |WHERE projects.id = ?
       |  AND users_courses.user_id = ?
     """.stripMargin

  val FindUserForTeacher =
    s"""
       |SELECT users.id as student_id, users.version as version, users.username as username, users.email as email,
       |       users.password_hash as password_hash, users.givenname as givenname, users.surname as surname,
       |       users.created_at as created_at, users.updated_at as updated_at
       |FROM users, $Table, users_courses
       |WHERE $Table.teacher_id = ?
       |  AND users_courses.user_id = ?
       |  AND $Table.id = users_courses.course_id
       |  AND users.id  = users_courses.user_id
     """.stripMargin

  /**
   * List all courses.
   *
   * @return an  array of Courses
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]] = {
    queryList(SelectAll)
  }

  /**
   * Return course by its project.
   *
   * @param project  the project to filter by
   * @return a result set
   */
  override def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]] = {
    queryList(ListCourseForProject, Seq[Any](project.id))
  }

  /**
   * Select courses based on the given user.
   *
   * @param user the user to search by
   * @param asTeacher  whether we are searching for courses this user teachers,
   *                   or courses this user is a student of.
   * @return the found courses
   */
  override def list(user: User, asTeacher: Boolean = false) // format: OFF
                   (implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]] = { // format: ON

    if (asTeacher) {
      val key = cacheTeachingKey(user.id)
      cache.getCached[IndexedSeq[Course]](key).flatMap {
        case \/-(courseList) => Future successful \/-(courseList)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            courseList <- lift(queryList(if (asTeacher) ListByTeacherId else ListCourses, Seq[Any](user.id)))
            _ <- lift(cache.putCache[IndexedSeq[Course]](key)(courseList, ttl))
          } yield courseList
        case -\/(error) => Future successful -\/(error)
      }
    }
    else {
      for {
        courseList <- lift(queryList(ListCourses, Seq[Any](user.id)))
      } yield courseList
    }
  }
  /**
   * List the courses associated with a user.
   */
  override def list(users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Map[UUID, IndexedSeq[Course]]]] = {
    val cleanUsersId = users.map { user =>
      user.id.toString filterNot ("-" contains _)
    }

    conn.sendPreparedStatement(ListCoursesForUserList, Array[Any](cleanUsersId)).map { queryResult =>
      try {
        queryResult.rows match {
          case Some(resultSet) => {
            val startTimeUsC = System.nanoTime() / 1000
            val tuples = queryResult.rows.get.map { item: RowData =>
              (item("user_id").asInstanceOf[UUID], constructor(item))
            }
            val tupledWithUsers = users.map { user =>
              (user.id, tuples.filter(_._1 == user.id).map(_._2))
            }
            \/-(tupledWithUsers.toMap)
          }
          case None => -\/(RepositoryError.NoResults(s"Could not list all courses for users in ${users.map(_.id).toString}"))
        }
      }
      catch {
        case exception: NoSuchElementException => throw exception
      }
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))

      case exception: GenericDatabaseException =>
        \/.left(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))

      case exception => throw exception
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Course]] = {
    cache.getCached[Course](cacheCourseKey(id)).flatMap {
      case \/-(course) => Future successful \/-(course)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          course <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cache.putCache[UUID](cacheCourseSlugKey(course.slug))(course.id, ttl))
          _ <- lift(cache.putCache[Course](cacheCourseKey(course.id))(course, ttl))
        } yield course
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param slug the course's slug
   * @return an optional RowData object containing the results
   */
  override def find(slug: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Course]] = {
    cache.getCached[UUID](cacheCourseSlugKey(slug)).flatMap {
      case \/-(courseId) => {
        for {
          _ <- lift(cache.putCache[UUID](cacheCourseSlugKey(slug))(courseId, ttl))
          course <- lift(find(courseId))
        } yield course
      }
      case -\/(noResults: RepositoryError.NoResults) => {
        for {
          course <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
          _ <- lift(cache.putCache[UUID](cacheCourseSlugKey(slug))(course.id, ttl))
          _ <- lift(cache.putCache[Course](cacheCourseKey(course.id))(course, ttl))
        } yield course
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Add a user to a course
   */
  override def addUser(user: User, course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(AddUser, Array(user.id, course.id, new DateTime))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not add ${user.id.toString} to course ${course.id.toString}"))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheStudentsKey(course.id)))
    } yield ()
  }

  /**
   * Remove a user from a course.
   */
  override def removeUser(user: User, course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(RemoveUser, Array(user.id, course.id))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheStudentsKey(course.id)))
    } yield ()
  }

  /**
   * Verify if this user has access to this project through any of his courses.
   *
   * @param user
   * @param project
   * @param conn
   * @return
   */
  override def hasProject(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]] = {
    conn.sendPreparedStatement(HasProject, Array[Any](project.id, user.id)).map { result =>
      if (result.rows.get.length > 0) {
        \/-(true)
      }
      else {
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
  override def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanCourseId = course.id.toString filterNot ("-" contains _)
    val query = AddUsers + users.map { user =>
      val cleanUserId = user.id.toString filterNot ("-" contains _)
      s"('$cleanCourseId', '$cleanUserId', '${new DateTime()}')"
    }.mkString(",")

    for {
      _ <- lift(queryNumRows(query)(users.length == _).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but the users could not be added to the course."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheStudentsKey(course.id)))
    } yield ()
  }

  /**
   * Remove users from a course.
   *
   * @param course  the course to remove users from.
   * @param users  an array of the users to be removed.
   * @param conn  an implicit database Connection.
   * @return a boolean indicating if the action was successful.
   */
  override def removeUsers(course: Course, users: IndexedSeq[User]) // format: OFF
                          (implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = { // format: ON
    val cleanCourseId = course.id.toString filterNot ("-" contains _)
    val cleanUsersId = users.map { user =>
      user.id.toString filterNot ("-" contains _)
    }

    for {
      _ <- lift(queryNumRows(RemoveUsers, Seq[Any](cleanCourseId, cleanUsersId))(users.length == _).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheStudentsKey(course.id)))
    } yield ()
  }

  /**
   * Remove all users from a course.
   *
   * @param course  the course to remove users from.
   * @param conn  an implicit database Connection.
   * @return a boolean indicating if the action was successful.
   */
  override def removeAllUsers(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(RemoveAllUsers, Seq[Any](course.id))(_ >= 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("No rows were affected"))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheStudentsKey(course.id)))
    } yield ()
  }

  /**
   * Insert a Course row.
   *
   * @param course
   * @param conn
   * @return
   */
  def insert(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Course]] = {
    val params = Seq[Any](
      course.id, 1, course.teacherId, course.name, course.color.getRGB, course.slug, course.id,
      course.enabled, course.archived, course.chatEnabled, course.schedulingEnabled, course.theaterMode, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cache.removeCached(cacheTeachingKey(course.teacherId)))
    } yield inserted
  }

  /**
   * Update a Course row
   *
   * @param course
   * @param conn
   * @return
   */
  override def update(course: Course) // format: OFF
                     (implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Course]] = { // format: ON
    val params = Seq[Any](
      course.version + 1, course.teacherId, course.name, course.color.getRGB, course.slug, course.id,
      course.enabled, course.archived, course.schedulingEnabled, course.theaterMode, course.chatEnabled, new DateTime, course.id, course.version
    )
    for {
      updated <- lift(queryOne(Update, params))
      students <- lift(userRepository.list(updated))
      _ <- lift(cache.removeCached(cacheStudentsKey(updated.id)))
      _ <- lift(cache.removeCached(cacheCourseKey(updated.id)))
      _ <- lift(cache.removeCached(cacheCourseSlugKey(updated.slug)))
      _ <- lift(cache.removeCached(cacheTeachingKey(course.teacherId)))
      _ <- liftSeq { students.map { student => cache.removeCached(cacheCoursesKey(student.id)) } }
    } yield updated
  }

  /**
   * Delete a Course row.
   *
   * @param course
   * @param conn
   * @return
   */
  override def delete(course: Course)  // format: OFF
                     (implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Course]] = { // format: ON
    for {
      deleted <- lift(queryOne(Delete, Array(course.id)))
      students <- lift(userRepository.list(deleted))
      _ <- lift(cache.removeCached(cacheStudentsKey(deleted.id)))
      _ <- lift(cache.removeCached(cacheCourseKey(deleted.id)))
      _ <- lift(cache.removeCached(cacheCourseSlugKey(deleted.slug)))
      _ <- lift(cache.removeCached(cacheTeachingKey(course.teacherId)))
      _ <- liftSeq { students.map { student => cache.removeCached(cacheCoursesKey(student.id)) } }
    } yield deleted
  }
}
