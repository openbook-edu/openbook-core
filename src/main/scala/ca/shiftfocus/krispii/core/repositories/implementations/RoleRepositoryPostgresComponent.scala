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

    val Restore = s"""
      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
    """

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
        WHERE name = ?
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
    override def list: Future[IndexedSeq[Role]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Role(item)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the roles associated with a user.
     */
    override def list(user: User): Future[IndexedSeq[Role]] = {
      db.pool.sendPreparedStatement(ListRoles, Array[Any](user.id.bytes)).map { queryResult =>
        val roleList = queryResult.rows.get.map {
          item: RowData => Role(item)
        }
        //Cache.set(cacheString, roleList.map(_.id))
        //val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
        //Cache.set(s"role_ids:user_ids", cachedUserRoles + user.id )
        roleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the roles associated with a user.
     */
    override def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Role]]] = {
      val arrayString = users.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${ListRolesForUserList} AND ARRAY[users_roles.user_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { queryResult =>
        val tuples = queryResult.rows.get.map { item: RowData =>
          (UUID(item("id").asInstanceOf[Array[Byte]]), Role(item))
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
    override def find(id: UUID): Future[Option[Role]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Role(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by name
     *
     * @param name the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(name: String): Future[Option[Role]] = {
      db.pool.sendPreparedStatement(SelectOneByName, Array[Any](name)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Role(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[Boolean] = {
      val cleanRoleId = role.id.string filterNot ("-" contains _)
      val query = AddUsers + userList.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"('\\x$cleanRoleId', '\\x$cleanUserId', '${new DateTime}')"
      }.mkString(",")

      val wasAdded = for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)

      wasAdded.recover {
        case exception => {
          throw exception
        }
      }
    }

    def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection) = {
      val cleanRoleId = role.id.string filterNot ("-" contains _)
      val arrayString = userList.map { user =>
        val cleanUserId = user.id.string filterNot ("-" contains _)
        s"decode('$cleanUserId', 'hex')"
      }.mkString("ARRAY[", ",", "]")

      val query = s"""${RemoveUsers} '\\x$cleanRoleId' AND ARRAY[user_id] <@ $arrayString"""

      val wasRemoved = for {
        result <- conn.sendQuery(query)
      }
      yield (result.rowsAffected > 0)

      wasRemoved.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Save a Role row.
     *
     * @return id of the saved/new role.
     */
    override def insert(role: Role)(implicit conn: Connection): Future[Role] = {
      Logger.debug("[RoleTDG.insert] - " + role.name)
      val future = for {
        result <- conn.sendPreparedStatement(Insert, Array(role.id.bytes, new DateTime, new DateTime, role.name))
      }
      yield Role(result.rows.get.head)

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Save a Role row.
     *
     * @return id of the saved/new role.
     */
    override def update(role: Role)(implicit conn: Connection): Future[Role] = {
      val future = for {
        result <- conn.sendPreparedStatement(Update, Array(role.name, (role.version + 1), new DateTime, role.id.bytes, role.version))
      }
      yield Role(result.rows.get.head)

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     *
     * @param role
     * @return
     */
    def delete(role: Role)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(role.id.bytes, role.version))
      }
      yield { queryResult.rowsAffected > 0 }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Associate a role to a user.
     */
    override def addToUser(user: User, role: Role)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(AddRole, Array[Any](user.id.bytes, role.id.bytes, new DateTime)).map { result =>
        //Cache.remove(s"role_ids:user:${user.id.string}")
        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Associate a role to a user.
     */
    override def addToUser(user: User, name: String)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(AddRoleByName, Array[Any](user.id.bytes, name, new DateTime)).map { result =>
        //Cache.remove(s"role_ids:user:${user.id.string}")
        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Remove a role from a user.
     */
    override def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveRole, Array[Any](user.id.bytes, role.id.bytes)).map { result =>
        //Cache.remove(s"role_ids:user:${user.id.string}")
        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Associate a role to a user.
     */
    override def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveRoleByName, Array[Any](user.id.bytes, name)).map { result =>
        //Cache.remove(s"role_ids:user:${user.id.string}")
        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Remove a role from all users.
     */
    override def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveFromAllUsers, Array[Any](role.id.bytes)).map { result =>
        // Remove the cached lists of roles for all users
//        val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
//        cachedUserRoles.map { userId => Cache.remove(s"role_ids:user:${userId.string}") }
//        Cache.remove(s"role_ids:user_ids")

        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Associate a role to a user.
     */
    override def removeFromAllUsers(name: String)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveFromAllUsersByName, Array[Any](name)).map { result =>
        // Remove the cached lists of roles for all users
//        val cachedUserRoles = Cache.getAs[Set[UUID]](s"role_ids:user_ids").getOrElse(Set())
//        cachedUserRoles.map { userId => Cache.remove(s"role_ids:user:${userId.string}") }
//        Cache.remove(s"role_ids:user_ids")

        (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
