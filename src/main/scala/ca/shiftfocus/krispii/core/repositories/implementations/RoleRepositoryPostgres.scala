package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.error._
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

class RoleRepositoryPostgres(val userRepository: UserRepository) extends RoleRepository with PostgresRepository[Role] {

  override def constructor(row: RowData): Role = {
    Role(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, name"
  val QMarks = "?, ?, ?, ?, ?"
  val Table = "roles"

  // User CRUD operations
  val SelectAll = s"""
    SELECT $Fields
    FROM $Table
  """

  val SelectOne = s"""
    SELECT $Fields
    FROM $Table
    WHERE id = ?
  """

  val SelectOneByName = s"""
    SELECT $Fields
    FROM $Table
    WHERE name = ?
    ORDER BY created_at ASC
    LIMIT 1
  """

  val Insert = {
    s"""
      INSERT INTO $Table ($Fields)
      VALUES ($QMarks)
      RETURNING $Fields
    """
  }

  val Update = {
    s"""
      UPDATE $Table
      SET name = ?, version = ?, updated_at = ?
      WHERE id = ?
        AND version = ?
      RETURNING $Fields
    """
  }

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
     """.stripMargin


  // ---- User->[Roles] relationship operations --------------------------------

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
   * List all roles.
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]] = {
    queryList(SelectAll)
  }

  /**
   * List the roles associated with a user.
   */
  override def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]] = {
    queryList(ListRoles, Array[Any](user.id.bytes))
  }

  /**
   * List the roles associated with users.
   */
  override def list(users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Map[UUID, IndexedSeq[Role]]]] = {
    val arrayString = users.map { user =>
      val cleanUserId = user.id.string filterNot ("-" contains _)
      s"decode('$cleanUserId', 'hex')"
    }.mkString("ARRAY[", ",", "]")
    val query = s"""${ListRolesForUserList} AND ARRAY[users_roles.user_id] <@ $arrayString"""

    conn.sendQuery(query).map { queryResult =>
      queryResult.rows match {
        case Some(resultSet) => {
          val tuples = resultSet.map { item: RowData =>
            (UUID(item("user_id").asInstanceOf[Array[Byte]]), constructor(item))
          }
          val tupledWithUsers = users.map { user =>
            (user.id, tuples.filter(_._1 == user.id).map(_._2))
          }
          \/-(tupledWithUsers.toMap)
        }
        case None => -\/(RepositoryError.NoResults("The query was successful but no ResultSet was returned."))
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    queryOne(SelectOne, Array[Any](id.bytes))
  }

  /**
   * Find a single entry by name
   *
   * @param name the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    queryOne(SelectOneByName, Array[Any](name))
  }

  /**
   * Add role to users
   * @param role
   * @param userList
   * @param conn
   * @return
   */
  def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    val cleanRoleId = role.id.string filterNot ("-" contains _)
    val query = AddUsers + userList.map { user =>
      val cleanUserId = user.id.string filterNot ("-" contains _)
      s"('\\x$cleanRoleId', '\\x$cleanUserId', '${new DateTime}')"
    }.mkString(",")

    queryNumRows(query)(userList.length == _).map {
      case \/-(wasSuccessful) => if (wasSuccessful) \/-(role)
                                 else -\/(RepositoryError.NoResults("Role couldn't be added to all users."))
      case -\/(error) => -\/(error)
    }.recover {
      case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
        case Some(nField) =>
          if (nField == "users_roles_pkey") -\/(RepositoryError.UniqueKeyConflict(s"User has already this role"))
          else if (nField == "users_roles_user_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"User doesn't exist"))
          else if (nField == "users_roles_role_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"Role doesn't exist"))
          else throw exception
        case _ => throw exception
      }
      case exception: Throwable => throw exception
    }
  }

  /**
   * Remove role from users
   * @param role
   * @param userList
   * @param conn
   * @return
   */
  def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    val cleanRoleId = role.id.string filterNot ("-" contains _)
    val arrayString = userList.map { user =>
      val cleanUserId = user.id.string filterNot ("-" contains _)
      s"decode('$cleanUserId', 'hex')"
    }.mkString("ARRAY[", ",", "]")

    val query = s"""${RemoveUsers} '\\x$cleanRoleId' AND ARRAY[user_id] <@ $arrayString"""

   queryNumRows(query)(userList.length == _).map {
     case \/-(wasSuccessful) => if (wasSuccessful) \/-(role)
                                else -\/(RepositoryError.NoResults("Role couldn't be added to all users."))
     case -\/(error) => -\/(error)
    }.recover {
      case exception: Throwable => throw exception
    }
  }

  /**
   * Save a Role row.
   *
   * @return id of the saved/new role.
   */
  override def insert(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    val params = Seq[Any](role.id.bytes, new DateTime, new DateTime, role.name)

    queryOne(Insert, params).recover {
      case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
        case Some(nField) if nField == "roles_pkey" => -\/(RepositoryError.UniqueKeyConflict(s"A row with key ${role.id.string} already exists"))
        case _ => throw exception
      }
      case exception: Throwable => throw exception
    }
  }

  /**
   * Update a Role.
   *
   * @return id of the saved/new role.
   */
  override def update(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    val params = Seq[Any](role.name, role.version + 1, new DateTime, role.id.bytes, role.version)

    queryOne(Update, params)
  }

  /**
   * Delete a role
   *
   * @param role
   * @return
   */
  def delete(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    queryOne(Delete, Array(role.id.bytes, role.version)).recover {
      case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
        case Some(nField) if nField == "roles_pkey" => -\/(RepositoryError.UniqueKeyConflict(s"Deletion of a role with id ${role.id.string} caused a GenericDatabaseException"))
        case _ => throw exception
      }
      case exception: Throwable => throw exception
    }
  }

  /**
   * Associate a role to a user by role object.
   */
  override def addToUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    val params = Seq[Any](user.id.bytes, role.id.bytes, new DateTime)
    val fResult = queryNumRows(AddRole, params)(1 == _)

    fResult.map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
      case -\/(error) => -\/(error)
    }.recover {
      case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
        case Some(nField) =>
          if (nField == "users_roles_pkey") -\/(RepositoryError.UniqueKeyConflict(s"User has already this role"))
          else if (nField == "users_roles_user_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"Referenced user with id ${user.id.string} doesn't exist"))
          else if (nField == "users_roles_role_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"Referenced role with id ${role.id.string} doesn't exist"))
          else  throw exception
        case _ => throw exception
      }
      case exception: Throwable => throw exception
    }
  }

  /**
   * Associate a role to a user by role name.
   */
  override def addToUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    conn.sendPreparedStatement(AddRoleByName, Array[Any](user.id.bytes, new DateTime, name))

    val params = Seq[Any](user.id.bytes, new DateTime, name)
    val fResult = queryNumRows(AddRoleByName, params)(1 == _)

    fResult.map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
      case -\/(error) => -\/(error)
    }.recover {
      case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
        case Some(nField) =>
          if (nField == "users_roles_pkey") -\/(RepositoryError.UniqueKeyConflict(s"User has already this role"))
          else if (nField == "users_roles_user_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"Referenced user with id ${user.id.string} doesn't exist"))
          else if (nField == "users_roles_role_id_fkey") -\/(RepositoryError.ForeignKeyConflict(s"Referenced role with name ${name} doesn't exist"))
          else  throw exception
        case _ => throw exception
      }
      case exception: Throwable => throw exception
    }
  }

  /**
   * Remove a role from a user by role object.
   */
  override def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    queryNumRows(RemoveRole, Seq(user.id.bytes, role.id.bytes))(1 == _).map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
      case -\/(error) => -\/(error)
    }
  }

  /**
   * Remove a role from a user by role name.
   */
  override def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    queryNumRows(RemoveRoleByName, Seq(user.id.bytes, name))(1 == _).map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
      case -\/(error) => -\/(error)
    }
  }

  /**
   * Remove a role from all users by role object.
   */
  override def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    queryNumRows(RemoveFromAllUsers, Seq[Any](role.id.bytes))(1 <= _).map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.NoResults("It appears that no users had this role, so it has been removed from no one. But the query was successful, so there's that."))
      case -\/(error) => -\/(error)
    }
  }

  /**
   * Remove a role from all users by role name.
   */
  override def removeFromAllUsers(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    queryNumRows(RemoveFromAllUsers, Seq[Any](name))(1 <= _).map {
      case \/-(true) => \/-( () )
      case \/-(false) => -\/(RepositoryError.NoResults("It appears that no users had this role, so it has been removed from no one. But the query was successful, so there's that."))
      case -\/(error) => -\/(error)
    }
  }
}
