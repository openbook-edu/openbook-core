package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ Connection, ResultSet, RowData }
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import play.api.i18n.Messages

import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scala.util.Try
import scalaz.{ -\/, \/, \/- }
import scalaz.syntax.either._
import scalacache._
import scalacache.redis._
import scala.concurrent.duration._

class UserRepositoryPostgres extends UserRepository with PostgresRepository[User] {

  override val entityName = "User"

  override def constructor(row: RowData): User = {
    User(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      email = row("email").asInstanceOf[String],
      username = row("username").asInstanceOf[String],
      hash = Try(row("password_hash")).map(_.asInstanceOf[String]).toOption,
      givenname = row("givenname").asInstanceOf[String],
      surname = row("surname").asInstanceOf[String],
      alias = Option(row("alias").asInstanceOf[String]) match {
      case Some(alias) => Some(alias)
      case _ => None
    },
      accountType = row("account_type").asInstanceOf[String],
      isDeleted = row("is_deleted").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "users"
  val Fields = "id, version, created_at, updated_at, username, email, password_hash, givenname, surname, alias, account_type, is_deleted"
  val FieldsWithoutTable = "id, version, created_at, updated_at, username, email, givenname, surname, alias, account_type, is_deleted"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val FieldsWithoutHash = FieldsWithTable.replace(s"${Table}.password_hash,", "")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.surname ASC, ${Table}.givenname ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table
       |WHERE is_deleted = FALSE
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectAllRange =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table
       |WHERE is_deleted = FALSE
       |ORDER BY created_at DESC
       |LIMIT ? OFFSET ?
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       |  AND is_deleted = FALSE
       |LIMIT 1
     """.stripMargin

  val SelectOneAdmin =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       |LIMIT 1
     """.stripMargin

  val SelectAllWithRole =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table, users_roles
       |WHERE users.id = users_roles.user_id
       |  AND users_roles.role_id = ?
       |  AND is_deleted = FALSE
     """.stripMargin

  /**
   * if you know how to make this query prettier, by all means, go ahead.
   */
  val SelectAllByKeyNotDeleted =
    s"""
       |SELECT $FieldsWithoutTable from (SELECT $FieldsWithoutTable, email <-> ? AS dist
       |FROM users
       |WHERE is_deleted = false
       |ORDER BY dist LIMIT 10) as sub  where dist < 0.9;
    """.stripMargin

  val SelectAllByKeyWithDeleted =
    s"""
       |SELECT $FieldsWithoutTable from (SELECT $FieldsWithoutTable, email <-> ? AS dist
       |FROM users
       |ORDER BY dist LIMIT 10) as sub  where dist < 0.9;
    """.stripMargin

  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $FieldsWithoutHash
    """.stripMargin
  }

  val UpdateNoPass = {
    s"""
       |UPDATE $Table
       |SET username = ?, email = ?, givenname = ?, surname = ?, alias = ?, account_type = ?, is_deleted = ?, version = ?, updated_at = current_timestamp
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $FieldsWithoutHash
    """.stripMargin
  }

  val UpdateWithPass = {
    s"""
       |UPDATE $Table
        |SET username = ?, email = ?, password_hash = ?, givenname = ?, surname = ?, alias = ?, account_type = ?, is_deleted = ?, version = ?, updated_at = current_timestamp
        |WHERE id = ?
        |  AND version = ?
        |RETURNING $FieldsWithoutHash
    """.stripMargin
  }

  val Delete =
    s"""
       |UPDATE $Table
       |SET is_deleted = TRUE, updated_at = current_timestamp, username = ?, email = ?
       |WHERE id = ?
       |AND version = ?
       |RETURNING $FieldsWithoutHash
     """.stripMargin

  val SelectOneByIdentifier =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE (email = ? OR username = ?)
       |AND is_deleted = FALSE
       |LIMIT 1
     """.stripMargin

  // sql LIKE statement is not working with parameters so we are using string replace
  val SelectDeletedByEmail =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE (email like '%{identifier}')
       |AND is_deleted = TRUE
       |ORDER BY created_at DESC
       |LIMIT 1
     """.stripMargin

  val SelectAllWithCourse =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table, users_courses
       |WHERE $Table.id = users_courses.user_id
       |  AND users_courses.course_id = ?
       |  AND is_deleted = FALSE
       |ORDER BY $OrderBy
    """.stripMargin

  // TODO - not used
  //  val ListUsersFilterByRolesAndCourses =
  //    s"""
  //       |SELECT users.id, users.version, username, email, givenname, surname, password_hash, users.created_at as created_at, users.updated_at as updated_at
  //       |FROM users, roles, users_roles, courses, users_courses
  //       |WHERE users.id = users_roles.user_id
  //       |  AND roles.id = users_roles.role_id
  //       |  AND roles.name = ANY (?::text[])
  //       |  AND users.id = users_courses.user_id
  //       |  AND courses.id = users_courses.course_id
  //       |  AND courses.name = ANY (?::text[])
  //       |GROUP BY users.id
  //       |ORDER BY $OrderBy
  //  """.stripMargin

  /**
   * List all users.
   *
   * @return a future disjunction containing either the users, or a failure
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAll)
  }

  override def listRange(limit: Int, offset: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAllRange, Array[Any](limit, offset))
  }

  /**
   * List users with a specified set of user Ids.
   *
   * @param userIds an IndexedSeq of UUID of the users to list.
   * @return a future disjunction containing either the users, or a failure
   */
  override def list(userIds: IndexedSeq[UUID])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    serializedT(userIds)(find(_)).map(_.map { userList =>
      userList.map { user =>
        user.copy(hash = None)
      }
    })
  }

  /**
   * List all users who have a role.
   *
   * @param role
   * @param conn
   * @return
   */
  override def list(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAllWithRole, Seq[Any](role.id))
  }

  /**
   * List users in a given course.
   *
   * @return a future disjunction containing either the users, or a failure
   */
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    cache.getCached[IndexedSeq[User]](cacheStudentsKey(course.id)).flatMap {
      case \/-(userList) => Future successful \/.right[RepositoryError.Fail, IndexedSeq[User]](userList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          userList <- lift(queryList(SelectAllWithCourse, Seq[Any](course.id)))
          _ <- lift(cache.putCache[IndexedSeq[User]](cacheUserKey(course.id))(userList, ttl))
        } yield userList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a user by ID.
   *
   * @param id the UUID of the user to search for.
   * @return a future disjunction containing either the user, or a failure
   */
  override def find(id: UUID, includeDeleted: Boolean = false)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]] = {
    cache.getCached[User](cacheUserKey(id)).flatMap {
      case \/-(user) => Future successful \/.right[RepositoryError.Fail, User](user)
      case -\/(noResults: RepositoryError.NoResults) => {
        val query = {
          if (includeDeleted) SelectOneAdmin
          else SelectOne
        }
        for {
          user <- lift(queryOne(query, Seq[Any](id)))
          _ <- lift(cache.putCache[UUID](cacheUsernameKey(user.username))(user.id, ttl))
          _ <- lift(cache.putCache[User](cacheUserKey(user.id))(user, ttl))
        } yield user
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a user by their identifiers.
   *
   * @param identifier a String representing their e-mail or username.
   * @return a future disjunction containing either the user, or a failure
   */
  override def find(identifier: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]] = {
    cache.getCached[UUID](cacheUsernameKey(identifier)).flatMap {
      case \/-(userId) => {
        for {
          _ <- lift(cache.putCache[UUID](cacheUsernameKey(identifier))(userId, ttl))
          user <- lift(find(userId))
        } yield user
      }
      case -\/(noResults: RepositoryError.NoResults) => {
        for {
          user <- lift(queryOne(SelectOneByIdentifier, Seq[Any](identifier, identifier)))
          _ <- lift(cache.putCache[UUID](cacheUsernameKey(identifier))(user.id, ttl))
          _ <- lift(cache.putCache[User](cacheUserKey(user.id))(user, ttl))
        } yield user
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a deleted user by their identifiers, using sql LIKE '%identifier'. I.E. email or surname should ends with identifier.
   * Be careful, because deleted user can be: deleted_1487883998_some.email@example.com,
   * and new user can be email@example.com, which will also match sql LIKE query: '%email@example.com'
   *
   * @param identifier a String representing their e-mail or username.
   * @return a future disjunction containing either the user, or a failure
   */
  override def findDeleted(identifier: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]] = {
    // sql LIKE statement is not working with parameters so we are using string replace
    queryOne(SelectDeletedByEmail.replace("{identifier}", identifier))
  }

  /**
   * Save a new User.
   *
   * Because we're using UUID, we assume we can always successfully create
   * a user. The failure conditions will be things that result in an exception.
   *
   * @param user the user to insert into the database
   * @return a future disjunction containing either the inserted user, or a failure
   */
  override def insert(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
    val params = Seq[Any](
      user.id, 1, new DateTime, new DateTime, user.username, user.email,
      user.hash, user.givenname, user.surname, user.alias, user.accountType, user.isDeleted
    )
    queryOne(Insert, params)
  }

  /**
   * Update an existing user.
   *
   * Because you already have a user object, we assume that this user exists in
   * the database and will return the updated user. If your user is out of date,
   * or it's no longer found, an exception should be thrown.
   *
   * @param user the user to update in the database
   * @return a future disjunction containing either the updated user, or a failure
   */
  override def update(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]] = {
    for {
      updated <- lift {
        user.hash match {
          case Some(hash) =>
            queryOne(UpdateWithPass, Seq[Any](
              user.username, user.email, hash, user.givenname, user.surname, user.alias, user.accountType, user.isDeleted,
              user.version + 1, user.id, user.version
            ))
          case None => queryOne(UpdateNoPass, Seq[Any](
            user.username, user.email, user.givenname, user.surname, user.alias, user.accountType, user.isDeleted,
            user.version + 1, user.id, user.version
          ))
        }
      }
      _ <- lift(cache.removeCached(cacheUserKey(updated.id)))
      _ <- lift(cache.removeCached(cacheUsernameKey(updated.username)))
    } yield updated
  }

  /**
   * Mark user as deleted and update username and email.
   *
   * @param user the user to be deleted
   * @return a future disjunction containing either the deleted user, or a failure
   */
  override def delete(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]] = {
    val timestamp = System.currentTimeMillis / 1000
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](
        (s"deleted_${timestamp}_" + user.username),
        (s"deleted_${timestamp}_" + user.email),
        user.id,
        user.version
      )))
      _ <- lift(cache.removeCached(cacheUserKey(deleted.id)))
      _ <- lift(cache.removeCached(cacheUsernameKey(deleted.username)))
    } yield deleted
  }

  /**
   * Search by triagrams for autocomplete
   * @param key
   * @param conn
   */
  def triagramSearch(key: String, includeDeleted: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    val query = {
      if (includeDeleted) SelectAllByKeyWithDeleted
      else SelectAllByKeyNotDeleted
    }

    queryList(query, Seq[Any](key))
  }
}
