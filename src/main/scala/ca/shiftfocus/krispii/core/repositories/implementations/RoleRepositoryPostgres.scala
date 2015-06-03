package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.lib.concurrent.Lifting
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import play.api.Play.current


import scala.Some
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import scalacache.ScalaCache
import scalaz.{\/, -\/, \/-}
import scalaz.syntax.either._

class RoleRepositoryPostgres(val userRepository: UserRepository) extends RoleRepository with PostgresRepository[Role] with Lifting[RepositoryError.Fail] {

  override def constructor(row: RowData): Role = {
    Role(
      row("id").asInstanceOf[UUID],
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
    |SELECT $Fields
    |FROM $Table
  """.stripMargin

  val SelectOne = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE id = ?
  """.stripMargin

  val SelectOneByName = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE name = ?
    |ORDER BY created_at ASC
    |LIMIT 1
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
      |SET name = ?, version = ?, updated_at = ?
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


  // ---- User->[Roles] relationship operations --------------------------------

  val AddRole = """
    |INSERT INTO users_roles (user_id, role_id, created_at)
    |VALUES (?, ?, ?)
  """.stripMargin

  val AddRoleByName = """
    |INSERT INTO users_roles (user_id, role_id, created_at)
    |  SELECT ? AS user_id, roles.id, ? AS created_at
    |  FROM roles
    |  WHERE roles.name = ?
  """.stripMargin

  val RemoveRole = """
    |DELETE FROM users_roles
    |WHERE user_id = ?
    |  AND role_id = ?
  """.stripMargin

  val RemoveRoleByName = """
    |DELETE FROM users_roles
    |WHERE user_id = ?
    |  AND role_id = (SELECT id FROM roles WHERE name = ?)
  """.stripMargin

  val RemoveFromAllUsers = """
    |DELETE FROM users_roles
    |WHERE role_id = ?
  """.stripMargin

  val RemoveFromAllUsersByName = """
    |DELETE FROM users_roles
    |WHERE role_id = (SELECT id FROM roles WHERE name = ? LIMIT 1)
  """.stripMargin

  val ListRoles = """
    |SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
    |FROM roles, users_roles
    |WHERE roles.id = users_roles.role_id
    |  AND users_roles.user_id = ?
  """.stripMargin

  val ListRolesForUserList = """
    |SELECT id, version, users_roles.user_id, roles.name as name, roles.created_at as created_at, updated_at
    |FROM roles, users_roles
    |WHERE roles.id = users_roles.role_id
  """.stripMargin

  val AddUsers = s"""
    |INSERT INTO users_roles (role_id, user_id, created_at)
    |VALUES
  """.stripMargin

  val RemoveUsers = s"""
    |DELETE FROM users_roles
    |WHERE role_id =
  """.stripMargin

  /**
   * List all roles.
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]] = {
    queryList(SelectAll)
  }

  /**
   * List the roles associated with a user.
   */
  override def list(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]] = {
    cache.getCached[IndexedSeq[Role]](cacheRolesKey(user.id)).flatMap {
      case \/-(roleList) => Future successful \/-(roleList)
      case -\/(RepositoryError.NoResults) =>
        for {
          roleList <- lift(queryList(ListRoles, Array[Any](user.id)))
          _ <- lift(cache.putCache[IndexedSeq[Role]](cacheRolesKey(user.id))(roleList, ttl))
        } yield roleList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Role]] = {
    cache.getCached[Role](cacheCourseKey(id)).flatMap {
      case \/-(role) => Future successful \/-(role)
      case -\/(RepositoryError.NoResults) =>
        for {
          role <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cache.putCache[UUID](cacheRoleNameKey(role.name))(role.id, ttl))
          _ <- lift(cache.putCache[Role](cacheRoleKey(role.id))(role, ttl))
        } yield role
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by name
   *
   * @param name the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(name: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Role]] = {
    cache.getCached[UUID](cacheRoleNameKey(name)).flatMap {
      case \/-(roleId) => {
        for {
          _ <- lift(cache.putCache[UUID](cacheRoleNameKey(name))(roleId, ttl))
          role <- lift(find(roleId))
        } yield role
      }
      case -\/(RepositoryError.NoResults) => {
        for {
          role <- lift(queryOne(SelectOneByName, Array[Any](name)))
          _ <- lift(cache.putCache[UUID](cacheRoleNameKey(name))(role.id, ttl))
          _ <- lift(cache.putCache[Role](cacheRoleKey(role.id))(role, ttl))
        } yield role
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Add role to users
   * @param role
   * @param userList
   * @param conn
   * @return
   */
  override def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanRoleId = role.id.toString filterNot ("-" contains _)
    val query = AddUsers + userList.map { user =>
      val cleanUserId = user.id.toString filterNot ("-" contains _)
      s"('\\x$cleanRoleId', '\\x$cleanUserId', '${new DateTime}')"
    }.mkString(",")

    for {
      _ <- lift(queryNumRows(query)(userList.length == _).map {
        case \/-(wasSuccessful) => if (wasSuccessful) \/-( () )
        else -\/(RepositoryError.DatabaseError("Role couldn't be added to all users."))
        case -\/(error) => -\/(error)
      })
      _ <- liftSeq { userList.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield ()
  }

  /**
   * Remove role from users
   * @param role
   * @param userList
   * @param conn
   * @return
   */
  override def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanRoleId = role.id.toString filterNot ("-" contains _)
    val arrayString = userList.map { user =>
      val cleanUserId = user.id.toString filterNot ("-" contains _)
      s"decode('$cleanUserId', 'hex')"
    }.mkString("ARRAY[", ",", "]")

    val query = s"""${RemoveUsers} '\\x$cleanRoleId' AND ARRAY[user_id] <@ $arrayString"""



    for {
      _ <- lift(queryNumRows(query)(userList.length == _).map {
        case \/-(wasSuccessful) => if (wasSuccessful) \/-( () )
        else -\/(RepositoryError.DatabaseError("Role couldn't be removed from all users."))
        case -\/(error) => -\/(error)
      }.recover {
        case exception: Throwable => throw exception
      })
      _ <- liftSeq { userList.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield ()
  }

  /**
   * Save a Role row.
   *
   * @return id of the saved/new role.
   */
  override def insert(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]] = {
    val params = Seq[Any](role.id, 1, new DateTime, new DateTime, role.name)

    queryOne(Insert, params)
  }

  /**
   * Update a Role.
   *
   * @return id of the saved/new role.
   */
  override def update(role: Role)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Role]] = {
    val params = Seq[Any](role.name, role.version + 1, new DateTime, role.id, role.version)

    for {
      updated <- lift(queryOne(Update, params))
      users <- lift(userRepository.list(role))
      _ <- lift(cache.removeCached(cacheRoleKey(role.id)))
      _ <- lift(cache.removeCached(cacheRoleNameKey(role.name)))
      _ <- liftSeq { users.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield updated
  }

  /**
   * Delete a role
   *
   * @param role
   * @return
   */
  override def delete(role: Role)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Role]] = {
    for {
      users <- lift(userRepository.list(role))
      deleted <- lift(queryOne(Delete, Array(role.id, role.version)))
      _ <- lift(cache.removeCached(cacheRoleKey(role.id)))
      _ <- lift(cache.removeCached(cacheRoleNameKey(role.name)))
      _ <- liftSeq { users.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield deleted
  }

  /**
   * Associate a role to a user by role object.
   */
  override def addToUser(user: User, role: Role)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    val params = Seq[Any](user.id, role.id, new DateTime)
    val fResult = queryNumRows(AddRole, params)(1 == _)

    for {
      _ <- lift(fResult.map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheRolesKey(user.id)))
    } yield ()
  }

  /**
   * Associate a role to a user by role name.
   */
  override def addToUser(user: User, name: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      role   <- lift(find(name))
      _      <- {
        val params = Seq[Any] (user.id, new DateTime, name)
        val fResult = queryNumRows(AddRoleByName, params) (1 == _)

        lift(fResult.map {
          case \/-(true)  => \/-(())
          case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
          case -\/(error) => -\/(error)
        })
      }
      _ <- lift(cache.removeCached(cacheRolesKey(user.id)))
    } yield ()
  }

  // TODO - replace RepositoryError.DatabaseError, when user doesn't have role
  /**
   * Remove a role from a user by role object.
   */
  override def removeFromUser(user: User, role: Role)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(RemoveRole, Seq(user.id, role.id))(1 == _).map {
        case \/-(true)  => \/-( () )
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheRolesKey(user.id)))
    } yield ()
  }

  /**
   * Remove a role from a user by role name.
   */
  override def removeFromUser(user: User, name: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      user <- lift(userRepository.find(user.id))
      role <- lift(find(name))
      _    <- lift(queryNumRows(RemoveRoleByName, Seq(user.id, name))(1 == _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cache.removeCached(cacheRolesKey(user.id)))
    } yield ()
  }

  /**
   * Remove a role from all users by role object.
   */
  override def removeFromAllUsers(role: Role)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      users <- lift(userRepository.list(role))
      _ <- lift(queryNumRows(RemoveFromAllUsers, Seq[Any](role.id))(1 <= _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(RepositoryError.DatabaseError("It appears that no users had this role, so it has been removed from no one. But the query was successful, so there's that."))
        case -\/(error) => -\/(error)
      })
      _ <- liftSeq { users.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield ()
  }

  /**
   * Remove a role from all users by role name.
   */
  override def removeFromAllUsers(name: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      role <- lift(find(name))
      users <- lift(userRepository.list(role))
      _ <- lift(queryNumRows(RemoveFromAllUsersByName, Seq[Any](name))(1 <= _).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("It appears that no users had this role, so it has been removed from no one. But the query was successful, so there's that."))
        case -\/(error) => -\/(error)
      })
      _ <- liftSeq { users.map { user => cache.removeCached(cacheRolesKey(user.id)) } }
    } yield ()
  }
}
