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
import scala.Some
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import scalaz.{\/, -\/, \/-}
import scalaz.syntax.either._

trait RoleRepositoryPostgresComponent extends RoleRepositoryComponent {
  self: PostgresDB =>

  /**
   * Override with this trait's version of the RoleRepository.
   */
  override val roleRepository: RoleRepository = new RoleRepositoryPSQL

  /**
   * An implementation of the RoleRepository class.
   */
  private class RoleRepositoryPSQL extends RoleRepository {
    def fields = Seq("name")
    def table = "roles"
    def orderBy = "name ASC"
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

    val SelectOneByName = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE name = ?
      ORDER BY created_at ASC
      LIMIT 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, created_at, updated_at, $extraFields)
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

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    // TODO - not used
//    val Restore = s"""
//      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
//    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    // User->[Roles] relationship operations
    val AddRole = """
      INSERT INTO users_roles (user_id, role_id, created_at)
      VALUES (?, ?, ?)
    """

    val AddRoleByName = """
      INSERT INTO users_roles (user_id, role_id, created_at)
        SELECT ? AS user_id, roles.id, ? AS created_at
        FROM roles
        WHERE roles.name = ?
    """

    val RemoveRole = """
      DELETE FROM users_roles
      WHERE user_id = ?
        AND role_id = ?
    """

    val RemoveRoleByName = """
      DELETE FROM users_roles
      WHERE user_id = ?
        AND role_id = (SELECT id FROM roles WHERE name = ?)
    """

    val RemoveFromAllUsers = """
      DELETE FROM users_roles
      WHERE role_id = ?
    """

    val RemoveFromAllUsersByName = """
      DELETE FROM users_roles
      WHERE role_id = (SELECT id FROM roles WHERE name = ? LIMIT 1)
    """

    val ListRoles = """
      SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
      FROM roles, users_roles
      WHERE roles.id = users_roles.role_id
        AND users_roles.user_id = ?
    """

    val ListRolesForUserList = """
      SELECT id, version, users_roles.user_id, roles.name as name, roles.created_at as created_at, updated_at
      FROM roles, users_roles
      WHERE roles.id = users_roles.role_id
    """

    val AddUsers = s"""
      INSERT INTO users_roles (role_id, user_id, created_at)
      VALUES
    """

    val RemoveUsers = s"""
      DELETE FROM users_roles
      WHERE role_id =
    """

    /**
     * Cache a role into the in-memory cache.
     *
     * @param role the [[Role]] to be cached
     * @return the [[Role]] that was cached
     */
//    private def cache(role: Role): Role = {
//      Cache.set(s"roles:id:${role.id}]", role, db.cacheExpiry)
//      role
//    }

    /**
     * Remove a role from the in-memory cache.
     *
     * @param role the [[Role]] to be uncached
     * @return the [[Role]] that was uncached
     */
//    private def uncache(role: Role): Role = {
//      Cache.remove(s"roles:id:${role.id}")
//      role
//    }

    /**
     * List all roles.
     */
    override def list: Future[\/[Fail, IndexedSeq[Role]]] = {
      db.pool.sendQuery(SelectAll).map {
        result => buildRoleList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * List the roles associated with a user.
     */
    override def list(user: User): Future[\/[Fail, IndexedSeq[Role]]] = {
      db.pool.sendPreparedStatement(ListRoles, Array[Any](user.id.bytes)).map {
        result => buildRoleList(result.rows)
        //Cache.set(cacheString, roleList.map(_.id))
        //val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
        //Cache.set(s"role_ids:user_ids", cachedUserRoles + user.id )
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * List the roles associated with a users.
     */
    override def list(users: IndexedSeq[User]): Future[\/[Fail, Map[UUID, IndexedSeq[Role]]]] = {
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${ListRolesForUserList} AND ARRAY[users_roles.user_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { queryResult =>
        try {
          queryResult.rows match {
            case Some(resultSet) => {
              val tuples = resultSet.map { item: RowData =>
                (UUID(item("user_id").asInstanceOf[Array[Byte]]), Role(item))
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
        case exception => {
          case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
        }
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(id: UUID): Future[\/[Fail, Role]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Find a single entry by name
     *
     * @param name the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(name: String): Future[\/[Fail, Role]] = {
      db.pool.sendPreparedStatement(SelectOneByName, Array[Any](name)).map {
        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }

    /**
     * Add role to users
     * @param role
     * @param userList
     * @param conn
     * @return
     */
    def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Role]] = {
      val cleanRoleId = role.id.string filterNot ("-" contains _)
      val query = AddUsers + userList.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"('\\x$cleanRoleId', '\\x$cleanUserId', '${new DateTime}')"
      }.mkString(",")

      val wasAdded = for {
        result <- conn.sendQuery(query)
      }
      yield if (result.rowsAffected == 0) {
        -\/(GenericFail("No rows were modified"))
      } else {
        \/-(role)
      }

      wasAdded.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_roles_pkey") -\/(EntityAlreadyExists(s"User has already this role"))
            else if (nField == "users_roles_user_id_fkey") -\/(EntityReferenceFieldError(s"User doesn't exist"))
            else if (nField == "users_roles_role_id_fkey") -\/(EntityReferenceFieldError(s"Role doesn't exist"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove role from users
     * @param role
     * @param userList
     * @param conn
     * @return
     */
    def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Role]] = {
      val cleanRoleId = role.id.string filterNot ("-" contains _)
      val arrayString = userList.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")

      val query = s"""${RemoveUsers} '\\x$cleanRoleId' AND ARRAY[user_id] <@ $arrayString"""

      val wasRemoved = for {
        result <- conn.sendQuery(query)
      }
      yield if (result.rowsAffected == 0) {
          -\/(GenericFail("No rows were modified"))
        } else {
          \/-(role)
        }

      wasRemoved.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Save a Role row.
     *
     * @return id of the saved/new role.
     */
    override def insert(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      Logger.debug("[RoleTDG.insert] - " + role.name)
      val future = for {
        result <- conn.sendPreparedStatement(Insert, Array(role.id.bytes, new DateTime, new DateTime, role.name))
      }
      yield buildRole(result.rows)

      future.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) if nField == "roles_pkey" => -\/(EntityAlreadyExists(s"A row with key ${role.id.string} already exists"))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update a Role.
     *
     * @return id of the saved/new role.
     */
    override def update(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      val future = for {
        result <- conn.sendPreparedStatement(Update, Array(role.name, (role.version + 1), new DateTime, role.id.bytes, role.version))
      }
      yield buildRole(result.rows)

      future.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     *
     * @param role
     * @return
     */
    def delete(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(role.id.bytes, role.version))
      }
      yield buildRole(result.rows)

      future.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) if nField == "roles_pkey" => -\/(EntityAlreadyExists(s"Deletion of a role with id ${role.id.string} caused a GenericDatabaseException"))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Associate a role to a user by role object.
     */
    override def addToUser(user: User, role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(AddRole, Array[Any](user.id.bytes, role.id.bytes, new DateTime)).map {
        result => buildRole(result.rows)
        //Cache.remove(s"role_ids:user:${user.id.string}")
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_roles_pkey") -\/(EntityAlreadyExists(s"User has already this role"))
            else if (nField == "users_roles_user_id_fkey") -\/(EntityReferenceFieldError(s"User doesn't exist"))
            else if (nField == "users_roles_role_id_fkey") -\/(EntityReferenceFieldError(s"Role doesn't exist"))
            else  -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Associate a role to a user by role name.
     */
    override def addToUser(user: User, name: String)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(AddRoleByName, Array[Any](user.id.bytes, new DateTime, name)).map {
        result => buildRole(result.rows)
        //Cache.remove(s"role_ids:user:${user.id.string}")
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "users_roles_pkey") -\/(EntityAlreadyExists(s"User has already this role"))
            else if (nField == "users_roles_user_id_fkey") -\/(EntityReferenceFieldError(s"User doesn't exist"))
            else if (nField == "users_roles_role_id_fkey") -\/(EntityReferenceFieldError(s"Role doesn't exist"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove a role from a user by role object.
     */
    override def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(RemoveRole, Array[Any](user.id.bytes, role.id.bytes)).map {
        //Cache.remove(s"role_ids:user:${user.id.string}")
        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove a role from a user by role name.
     */
    override def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(RemoveRoleByName, Array[Any](user.id.bytes, name)).map {
        //Cache.remove(s"role_ids:user:${user.id.string}")
        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove a role from all users by role object.
     */
    override def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(RemoveFromAllUsers, Array[Any](role.id.bytes)).map {
        // Remove the cached lists of roles for all users
//        val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
//        cachedUserRoles.map { userId => Cache.remove(s"role_ids:user:${userId.string}") }
//        Cache.remove(s"role_ids:user_ids")

        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Remove a role from all users by role name.
     */
    override def removeFromAllUsers(name: String)(implicit conn: Connection): Future[\/[Fail, Role]] = {
      conn.sendPreparedStatement(RemoveFromAllUsersByName, Array[Any](name)).map {
        // Remove the cached lists of roles for all users
//        val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
//        cachedUserRoles.map { userId => Cache.remove(s"role_ids:user:${userId.string}") }
//        Cache.remove(s"role_ids:user_ids")

        result => buildRole(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }


    /**
     * Transform result rows into a single role.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildRole(maybeResultSet: Option[ResultSet]): \/[Fail, Role] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(Role(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Role from the row returned.", exception))
      }
    }

    /**
     * Converts an optional result set into role list
     *
     * @param maybeResultSet
     * @return
     */
    private def buildRoleList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[Role]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(Role.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Role List from the rows returned.", exception))
      }
    }
  }
}
