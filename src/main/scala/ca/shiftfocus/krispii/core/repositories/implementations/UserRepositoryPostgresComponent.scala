package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{RowData, Connection, ResultSet}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.i18n.Messages
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scalaz.{\/, -\/, \/-}
import scalaz.syntax.either._

trait UserRepositoryPostgresComponent extends UserRepositoryComponent {
  // Because this concrete implementation is postgres specific, we will specifically
  // depend on the PostgresDB trait.
  self: PostgresDB =>

  /**
   * The userRepository value is overriden with the implementation provided
   * below. When this concrete implementation trait is injected via the controller,
   * the userRepository value will be instantiated with this class.
   */
  override val userRepository: UserRepository = new UserRepositoryPSQL

  /**
   * The implementation class of the UserRepository.
   */
  private class UserRepositoryPSQL extends UserRepository with PostgresRepository[User] {

    override def constructor(row: RowData): User = {
      User(
        id           = UUID(row("id").asInstanceOf[Array[Byte]]),
        version      = row("version").asInstanceOf[Long],
        email        = row("email").asInstanceOf[String],
        username     = row("username").asInstanceOf[String],
        hash = Option(row("password_hash")) match {
          case Some(cell) => Some(cell.asInstanceOf[String])
          case None => None
        },
        givenname    = row("givenname").asInstanceOf[String],
        surname      = row("surname").asInstanceOf[String],
        createdAt    = row("created_at").asInstanceOf[DateTime],
        updatedAt    = row("updated_at").asInstanceOf[DateTime]
      )
    }

    val Fields = "id, version, created_at, updated_at, username, email, password_hash, givenname, surname"
    val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?"
    val Table = "users"
    val OrderBy = "surname ASC, givenname ASC"

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

    val SelectAllWithRole =
      s"""
         |SELECT $Fields
         |FROM $Table, users_roles
         |WHERE users.id = users_roles.id
         |  AND users_roles.role_id = ?
       """.stripMargin

    val Insert = {
      s"""
         |INSERT INTO $Table ($Fields)
         |VALUES ($QMarks)
         |RETURNING $Fields
      """.stripMargin
    }

    val Update = {
      s"""
         |UPDATE $Table
         |SET username = ?, email = ?, password_hash = ?, givenname = ?, surname = ?, version = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |RETURNING $Fields
      """.stripMargin
    }

    val Delete =
      s"""
         |DELETE FROM $Table
         |WHERE id = ?
         |  AND version = ?
         |RETURNING $Fields
       """.stripMargin

    val SelectOneEmail =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE email = ?
       """.stripMargin

    val SelectOneByIdentifier =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE (email = ? OR username = ?)
         |LIMIT 1
       """.stripMargin

    val SelectAllWithCourse = s"""
      SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, users_courses
      WHERE users.id = users_courses.user_id
        AND users_courses.course_id = ?
      ORDER BY $OrderBy
    """

    val ListUsersFilterByRoles = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, roles, users_roles
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
      GROUP BY users.id
      ORDER BY $OrderBy
    """

    // TODO - not used
//    val ListUsersFilterByCourses = s"""
//      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
//      FROM users, classes, users_classes
//      WHERE users.id = users_classes.user_id
//        AND classes.id = users_classes.class_id
//    """

    val ListUsersFilterByRolesAndCourses = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, roles, users_roles, courses, users_courses
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
        AND users.id = users_courses.user_id
        AND courses.id = users_courses.course_id
        AND courses.name = ANY (?::text[])
      GROUP BY users.id
      ORDER BY $OrderBy
    """

    /**
     * List all users.
     *
     * @return a future disjunction containing either the users, or a failure
     */
    override def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[User]]] = {
      queryList(SelectAll)
    }

    /**
     * List users with a specified set of user Ids.
     *
     * @param userIds an [[IndexedSeq]] of [[UUID]] of the users to list.
     * @return a future disjunction containing either the users, or a failure
     */
    override def list(userIds: IndexedSeq[UUID])(implicit conn: Connection): Future[\/[Fail, IndexedSeq[User]]] = {
      Future.sequence {
        userIds.map { userId =>
          find(userId).map(_.toOption.get)
        }
      }.map {
        users => \/-(users)
      }
    }

    /**
     * List all users who have a role.
     *
     * @param role
     * @param conn
     * @return
     */
    override def list(role: Role)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[User]]] = {
      queryList(SelectAllWithRole, role.id.bytes)
    }

    /**
     * List users in a given course.
     *
     * @return a future disjunction containing either the users, or a failure
     */
    override def list(course: Course)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[User]]] = {
      queryList(SelectAllWithCourse, Seq[Any](course.id.bytes))
    }

    /**
     * Find a user by ID.
     *
     * @param id the [[UUID]] of the user to search for.
     * @return a future disjunction containing either the user, or a failure
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, User]] = {
      queryOne(SelectOne, Seq[Any](id.bytes))
    }

    /**
     * Find a user by their identifiers.
     *
     * @param identifier a [[String]] representing their e-mail or username.
     * @return a future disjunction containing either the user, or a failure
     */
    override def find(identifier: String)(implicit conn: Connection): Future[\/[Fail, User]] = {
      queryOne(SelectOneByIdentifier, Seq[Any](identifier, identifier))
    }

    /**
     * Find a user by e-mail address.
     *
     * @param email the e-mail address of the user to find
     * @return a future disjunction containing either the user, or a failure
     */
    override def findByEmail(email: String)(implicit conn: Connection): Future[\/[Fail, User]] = {
      queryOne(SelectOneEmail, Seq[Any](email))
    }

    /**
     * Save a new User.
     *
     * Because we're using UUID, we assume we can always successfully create
     * a user. The failure conditions will be things that result in an exception.
     *
     * @param user the [[User]] to insert into the database
     * @return a future disjunction containing either the inserted user, or a failure
     */
    override def insert(user: User)(implicit conn: Connection): Future[\/[Fail, User]] = {
      val params = Seq[Any](
        user.id.bytes, 1, new DateTime, new DateTime, user.username, user.email,
        user.hash, user.givenname, user.surname
      )

      queryOne(Insert, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_pkey") -\/(UniqueFieldConflict(s"A row with key ${user.id.string} already exists"))
            // TODO - check nField name
            else if (nField == "users_username_key") -\/(UniqueFieldConflict(s"User with username ${user.username} already exists"))
            else if (nField == "users_email_key") -\/(UniqueFieldConflict(s"User with email ${user.username} already exists"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Update an existing user.
     *
     * Because you already have a user object, we assume that this user exists in
     * the database and will return the updated user. If your user is out of date,
     * or it's no longer found, an exception should be thrown.
     *
     * @param user the [[User]] to update in the database
     * @return a future disjunction containing either the updated user, or a failure
     */
    override def update(user: User)(implicit conn: Connection): Future[\/[Fail, User]] = {
      val params = Seq[Any](
        user.username, user.email, user.hash.get, user.givenname, user.surname,
        user.version + 1, new DateTime, user.id.bytes, user.version
      )

      queryOne(Update, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_pkey") -\/(UniqueFieldConflict(s"A row with key ${user.id.string} already exists"))
            // TODO - check nField name
            else if (nField == "users_username_key") -\/(UniqueFieldConflict(s"User with username ${user.username} already exists"))
            else if (nField == "users_email_key") -\/(UniqueFieldConflict(s"User with email ${user.email} already exists"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Delete a user from the database.
     *
     * @param user the [[User]] to be deleted
     * @return a future disjunction containing either the deleted user, or a failure
     */
    override def delete(user: User)(implicit conn: Connection): Future[\/[Fail, User]] = {
      queryOne(Delete, Seq[Any](user.id.bytes, user.version)).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            // TODO - check nField name
            if (nField == "courses_teacher_id_fkey") -\/(ReferenceConflict(Messages("repositories.UserRepository.delete.refCourseError")))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }
  }
}
