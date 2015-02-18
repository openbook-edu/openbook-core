package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.{FatalError, RepositoryError}
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{RowData, Connection, ResultSet}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import error._
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
  private class UserRepositoryPSQL extends UserRepository {
    def fields = Seq("username", "email", "password_hash", "givenname", "surname")
    def table = "users"
    def orderBy = "surname ASC, givenname ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
    """

    val Insert = {
      s"""
        INSERT INTO $table (id, version, created_at, updated_at, $fieldsText)
        VALUES (?, 1, ?, ?, $questions)
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
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectOneEmail = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM users
      WHERE email = ?
    """

    val SelectOneByIdentifier = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM users
      WHERE (email = ? OR username = ?)
      LIMIT 1
    """

    val ListUsers = s"""
      SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, users_classes
      WHERE users.id = users_classes.user_id
        AND users_classes.class_id = ?
      ORDER BY $orderBy
    """

    val ListUsersFilterByRoles = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, roles, users_roles
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
      GROUP BY users.id
      ORDER BY $orderBy
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
      FROM users, roles, users_roles, classes, users_classes
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
        AND users.id = users_classes.user_id
        AND classes.id = users_classes.class_id
        AND classes.name = ANY (?::text[])
      GROUP BY users.id
      ORDER BY $orderBy
    """

    /**
     * List all users.
     *
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def list: Future[\/[RepositoryError, IndexedSeq[User]]] = {
      db.pool.sendQuery(SelectAll).map {
        result => buildUserList(result.rows)
      }.recover {
        case exception: Throwable => -\/(FatalError("An unexpected error occurred.", exception))
      }
    }

    /**
     * List users with a specified set of user Ids.
     *
     * @param userIds an [[IndexedSeq]] of [[UUID]] of the users to list.
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def list(userIds: IndexedSeq[UUID]): Future[\/[RepositoryError, IndexedSeq[User]]] = {
      Future.sequence {
        userIds.map { userId =>
          find(userId).map(_.toOption.get)
        }
      }.map {
        users => \/-(users)
      }.recover {
        case exception: NoSuchElementException => -\/(NoResultsFound("One or more users could not be loaded."))
        case exception: Throwable => -\/(FatalError("An unexpected error occurred.", exception))
      }
    }

    /**
     * List users in a given course.
     */
    override def list(course: Course): Future[\/[RepositoryError, IndexedSeq[User]]] = {
      db.pool.sendPreparedStatement(ListUsers, Seq[Any](course.id.bytes)).map {
        result => buildUserList(result.rows)
      }.recover {
        case exception: Throwable => -\/(FatalError("An unexpected error occurred.", exception))
      }
    }

    /**
     * List the users belonging to a set of classes.
     *
     * @param classes an [[IndexedSeq]] of [[Course]] to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
//    override def listForCourses(classes: IndexedSeq[Course]): Future[\/[RepositoryError, IndexedSeq[User]]] = {
//      Future.sequence(classes.map(list)).map(_.flatten).recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

    /**
     * List the users one of a set of roles.
     *
     * @param roles an [[IndexedSeq]] of [[String]] naming the roles to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
//    override def listForRoles(roles: IndexedSeq[String]): Future[\/[RepositoryError, IndexedSeq[User]]] = {
//      db.pool.sendPreparedStatement(ListUsersFilterByRoles, Array[Any](roles)).map { queryResult =>
//        queryResult.rows.get.map {
//          item: RowData => User(item)
//        }
//      }.recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

    /**
     * List users filtering by both roles and classes.
     *
     * @param roles an [[IndexedSeq]] of [[String]] naming the roles to filter by.
     * @param classes an [[IndexedSeq]] of [[Course]] to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
//    override def listForRolesAndCourses(roles: IndexedSeq[String], classes: IndexedSeq[String]): Future[\/[RepositoryError, IndexedSeq[User]]] = {
//      db.pool.sendPreparedStatement(ListUsersFilterByRolesAndCourses, Array[Any](roles, classes)).map { result =>
//        rowsToUsers(result.rows)
//      }.recover {
//        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
//      }
//    }

    /**
     * Find a user by ID.
     *
     * @param id the [[UUID]] of the user to search for.
     * @return an [[Option[User]]]
     */
    override def find(id: UUID): Future[\/[RepositoryError, User]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildUser(result.rows)
      }.recover {
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
      }
    }

    /**
     * Find a user by their identifiers.
     *
     * @param identifier a [[String]] representing their e-mail or username.
     * @return an [[Option[User]]]
     */
    override def find(identifier: String): Future[\/[RepositoryError, User]] = {
      db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](identifier, identifier)).map {
        result => buildUser(result.rows)
      }.recover {
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
      }
    }

    /**
     * Find a user by e-mail address.
     *
     * @param email the e-mail address of the user to find
     * @return an [[Option[User]]]
     */
    override def findByEmail(email: String): Future[\/[RepositoryError, User]] = {
      db.pool.sendPreparedStatement(SelectOneEmail, Array[Any](email)).map {
        result => buildUser(result.rows)
      }.recover {
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
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
     * @return the updated [[User]]
     */
    def update(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]] = {
      conn.sendPreparedStatement(Update, Array[Any](
        user.username,
        user.email,
        user.passwordHash.get,
        user.givenname,
        user.surname,
        user.version + 1,
        new DateTime,
        user.id.bytes,
        user.version)
      ).map {
        result => buildUser(result.rows)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField)
            if nField == "users_pkey" => -\/(PrimaryKeyExists(s"A row with key ${user.id.string} already exists"))
            // TODO - check nField name
            else if nField == "users_username_ukey" => -\/(UniqueKeyExists(s"User with username ${user.username} already exists"))
            else if nField == "users_email_ukey" => -\/(UniqueKeyExists(s"User with email ${user.username} already exists"))
            else => -\/(FatalError(s"Unknown db error", exception))
          case _ => -\/(FatalError("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
      }
    }

    /**
     * Save a new User.
     *
     * Because we're using UUID, we assume we can always successfully create
     * a user. The failure conditions will be things that result in an exception.
     *
     * @param user the [[User]] to insert into the database
     * @return the newly created [[User]]
     */
    def insert(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]] = {
      val future = for {
        result <- conn.sendPreparedStatement(Insert, Array(
          user.id.bytes,
          new DateTime,
          new DateTime,
          user.username,
          user.email,
          user.passwordHash,
          user.givenname,
          user.surname
        ))
      }
      yield buildUser(result.rows)

      future.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField)
            if nField == "users_pkey" => -\/(PrimaryKeyExists(s"A row with key ${user.id.string} already exists"))
            // TODO - check nField name
            else if nField == "users_username_ukey" => -\/(UniqueKeyExists(s"User with username ${user.username} already exists"))
            else if nField == "users_email_ukey" => -\/(UniqueKeyExists(s"User with email ${user.username} already exists"))
            else => -\/(FatalError(s"Unknown db error", exception))
          case _ => -\/(FatalError("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
      }
    }

    /**
     * Delete a user from the database.
     *
     * @param user the [[User]] to be deleted
     * @return a [[Boolean]] indicating success or failure
     */
    def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]] = {
      conn.sendPreparedStatement(Purge, Array(user.id.bytes, user.version)).map { result =>
        if (result.rowsAffected == 1) \/-(user)
        else -\/(NoResultsFound("Could not find a user row to delete."))
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField)
            // TODO - check nField name
            if nField == "courses_teacher_id_fkey" => -\/(FKViolation(s"User is teacher and has references in courses table, that has project"))
            else => -\/(FatalError(s"Unknown db error", exception))
          case _ => -\/(FatalError("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(FatalError("Unexpected exception", exception))
      }
    }



    /**
     * Transform result rows into a single user.
     *
     * @param maybeRows
     * @return
     */
    private def buildUser(maybeResultSet: Option[ResultSet]): \/[RepositoryError, User] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(User(firstRow))
            case None => -\/(NoResultsFound("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResultsFound("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(FatalError(s"Invalid data: could not build a user from the row returned.", exception))
      }
    }

    /**
     * Converts an optional result set into
     *
     * @param maybeRows
     * @return
     */
    private def buildUserList(maybeResultSet: Option[ResultSet]): \/[RepositoryError, IndexedSeq[User]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(User.apply))
          case None => -\/(NoResultsFound("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(FatalError(s"Invalid data: could not build a Users List from the rows returned.", exception))
      }
    }
  }
}
