package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.common.models._
import play.api.Play.current
import play.api.cache.Cache
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.common.services.datasource.PostgresDB

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
      WHERE status = 1
      ORDER BY $orderBy
    """

    val SelectWithIds = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
        AND status = 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
        VALUES (?, 1, 1, ?, ?, $questions)
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
          AND status = 1
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      UPDATE $table SET status = 0 WHERE id = ? AND version = ?
    """

    val Restore = s"""
      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val UpdateNoPass = {
      val extraFields = fields.filter(_ != "password_hash").map(" " + _ + " = ? ").mkString(",")
      s"""
        UPDATE users
        SET $extraFields , version = ?, updated_at = ?
        WHERE id = ?
          AND version = ?
          AND status = 1
        RETURNING version
      """
    }

    val SelectOneEmail = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM users
      WHERE email = ?
        AND status = 1
    """

    val SelectOneByIdentifier = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM users
      WHERE (email = ? OR username = ?)
        AND status = 1
      LIMIT 1
    """

    val AddUser = """
      INSERT INTO users_sections (user_id, section_id, created_at)
      VALUES (?, ?, ?)
    """

    val RemoveUser = """
      DELETE FROM users_sections
      WHERE user_id = ?
        AND section_id = ?
    """

    val ListUsers = s"""
      SELECT id, version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, users_sections
      WHERE users.id = users_sections.user_id
        AND users_sections.section_id = ?
        AND users.status = 1
      ORDER BY $orderBy
    """

    val ListUsersFilterByRoles = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, roles, users_roles
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
        AND users.status = 1
        AND roles.status = 1
      GROUP BY users.id
      ORDER BY $orderBy
    """

    val ListUsersFilterBySections = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, sections, users_sections
      WHERE users.id = users_sections.user_id
        AND sections.id = users_sections.section_id
        AND users.status = 1
        AND sections.status = 1
    """

    val ListUsersFilterByRolesAndSections = s"""
      SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
      FROM users, roles, users_roles, sections, users_sections
      WHERE users.id = users_roles.user_id
        AND roles.id = users_roles.role_id
        AND roles.name = ANY (?::text[])
        AND users.id = users_sections.user_id
        AND sections.id = users_sections.section_id
        AND sections.name = ANY (?::text[])
        AND users.status = 1
        AND roles.status = 1
        AND sections.status = 1
      GROUP BY users.id
      ORDER BY $orderBy
    """

    val HasProject = s"""
      SELECT projects.id
      FROM users_sections
      INNER JOIN sections_projects ON users_sections.section_id = sections_projects.section_id
      INNER JOIN projects ON sections_projects.project_id = projects.id
      WHERE sections_projects.project_id = ?
        AND users_sections.user_id = ?
        AND projects.status = 1
    """

    /**
     * Cache a user into the in-memory cache.
     *
     * @param user the [[User]] to be cached
     * @return the [[User]] that was cached
     */
    private def cache(user: User): User = {
      Logger.debug(s"[userRepository.cache] - caching user ${user.username}")
      Cache.set(s"users[${user.id.string}]", user, db.cacheExpiry)
      Cache.set(s"user.id[${user.email}]", user.id, db.cacheExpiry)
      Cache.set(s"user.id[${user.username}]", user.id, db.cacheExpiry)
      user
    }

    /**
     * Remove a user from the in-memory cache.
     *
     * @param user the [[User]] to be uncached
     * @return the [[User]] that was uncached
     */
    private def uncache(user: User): User = {
      Cache.remove(s"users[${user.id.string}]")
      Cache.remove(s"user.id[${user.email}]")
      Cache.remove(s"user.id[${user.username}]")
      user
    }

    /**
     * List all users.
     *
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def list: Future[IndexedSeq[User]] = {
      Logger.debug("[userRepository.list] - Get all users")
      db.pool.sendQuery(SelectAll).map { queryResult =>
        Logger.debug("[userRepository.list] - Got query result")
        val startTimeUs = System.nanoTime() / 1000
        val userList = queryResult.rows.get.map {
          item: RowData => User(item)
        }
        val endTimeUs = System.nanoTime() / 1000
        Logger.debug(s"It took ${endTimeUs - startTimeUs} microseconds to instantiate your users.")
        Logger.debug("[userRepository.list] - done caching")
        //Cache.set(s"users.list", userList.map(_.id), db.cacheExpiry)
        userList
      }.recover {
        case exception => {
          Logger.error("Exception caught from userRepository.list")
          throw exception
        }
      }
    }

    /**
     * List users with a specified set of user Ids.
     *
     * @param userIds an [[IndexedSeq]] of [[UUID]] of the users to list.
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def list(userIds: IndexedSeq[UUID]): Future[IndexedSeq[User]] = {
      Future.sequence {
        userIds.map { userId =>
          find(userId).map(_.get)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List users in a given section.
     *
     *
     */
    override def list(section: Section): Future[IndexedSeq[User]] = {
      db.pool.sendPreparedStatement(ListUsers, Array(section.id.bytes)).map { queryResult =>
        val userList = queryResult.rows.get.map {
          item: RowData => {
            cache(User(item))
          }
        }
        Cache.set(s"users.list.section[${section.id.string}]", userList.map(_.id), db.cacheExpiry)
        userList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the users belonging to a set of sections.
     *
     * @param sections an [[IndexedSeq]] of [[Section]] to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def listForSections(sections: IndexedSeq[Section]) = {
      Future.sequence(sections.map(list)).map(_.flatten).recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List the users one of a set of roles.
     *
     * @param roles an [[IndexedSeq]] of [[String]] naming the roles to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def listForRoles(roles: IndexedSeq[String]) = {
      db.pool.sendPreparedStatement(ListUsersFilterByRoles, Array[Any](roles)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => cache(User(item))
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List users filtering by both roles and sections.
     *
     * @param roles an [[IndexedSeq]] of [[String]] naming the roles to filter by.
     * @param sections an [[IndexedSeq]] of [[Section]] to filter by.
     * @return an [[IndexedSeq]] of [[User]]
     */
    override def listForRolesAndSections(roles: IndexedSeq[String], sections: IndexedSeq[String]) = {
      db.pool.sendPreparedStatement(ListUsersFilterByRolesAndSections, Array[Any](roles, sections)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => cache(User(item))
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a user by ID.
     *
     * @param id the [[UUID]] of the user to search for.
     * @return an [[Option[User]]]
     */
    override def find(id: UUID): Future[Option[User]] = {
      Logger.debug(s"userRepository.find(${id.string}) - start")
      Cache.getAs[User](s"users[${id.string}]") match {
        case Some(user) => {
          Logger.debug(s"userRepository.find(${id.string}) - loaded from cache")
          Future.successful(Some(user))
        }
        case None => {
          db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
            result.rows.get.headOption match {
              case Some(rowData) => {
                Logger.debug(s"userRepository.find(${id.string}) - loaded from database")
                Some(cache(User(rowData)))
              }
              case None => {
                Logger.debug(s"userRepository.find(${id.string}) - user not found")
                None
              }
            }
          }
        }
      }
    }.recover {
      case exception => {
        Logger.debug(s"userRepository.find(${id.string}) - caught exception in db thread")
        throw exception
      }
    }

    /**
     * Find a user by their identifiers.
     *
     * @param identifier a [[String]] representing their e-mail or username.
     * @return an [[Option[User]]]
     */
    override def find(identifier: String): Future[Option[User]] = {
      Cache.getAs[UUID](s"user.id[${identifier}]") match {
        case Some(userId) => find(userId)
        case None => {
          db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](identifier, identifier)).map { result =>
            result.rows.get.headOption match {
              case Some(rowData) => {
                Logger.debug(s"userRepository.find(${identifier}) - loaded from database")
                Some(cache(User(rowData)))
              }
              case None => None
            }
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
      }
    }

    /**
     * Find a user by e-mail address.
     *
     * @param email the e-mail address of the user to find
     * @return an [[Option[User]]]
     */
    override def findByEmail(email: String): Future[Option[User]] = {
      Cache.getAs[UUID](s"user.id[${email}]") match {
        case Some(userId) => find(userId)
        case None => {
          db.pool.sendPreparedStatement(SelectOneEmail, Array[Any](email)).map { result =>
            result.rows.get.headOption match {
              case Some(rowData) => Some(cache(User(rowData)))
              case None => None
            }
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
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
    def update(user: User)(implicit conn: Connection): Future[User] = {
      conn.sendPreparedStatement(Update, Array[Any](
        user.username,
        user.email,
        user.passwordHash.get,
        user.givenname,
        user.surname,
        (user.version + 1),
        new DateTime,
        user.id.bytes,
        user.version)
      ).map {
        result => cache(User(result.rows.get.head))
      }.recover {
        case exception => {
          throw exception
        }
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
    def insert(user: User)(implicit conn: Connection): Future[User] = {
      Logger.debug("[UserTDG.insert]")

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
      yield cache(User(result.rows.get.head))

      future.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a user from the database.
     *
     * @param user the [[User]] to be deleted
     * @return a [[Boolean]] indicating success or failure
     */
    def delete(user: User)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(user.id.bytes, user.version))
      }
      yield {
        uncache(user)
        queryResult.rowsAffected > 0
      }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
