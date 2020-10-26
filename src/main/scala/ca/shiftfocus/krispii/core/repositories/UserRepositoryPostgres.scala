package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.{Course, Team}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaz.{-\/, \/, \/-}

class UserRepositoryPostgres(
    val tagRepository: TagRepository,
    val cacheRepository: CacheRepository
) extends UserRepository with PostgresRepository[User] {

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
  def FieldsWithTable(table: String = Table) = Fields.split(", ").map({ field => s"${table}." + field }).mkString(", ")
  val FieldsWithoutHash = FieldsWithTable().replace(s"${Table}.password_hash,", "")
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
  def SelectAllByKeyNotDeleted(key: String, limit: String, offset: Int) =
    s"""
     |SELECT $FieldsWithoutTable from (SELECT $FieldsWithoutTable, email <-> '$key' AS dist
     |FROM users
     |WHERE is_deleted = false
     |ORDER BY dist LIMIT $limit OFFSET $offset) as sub  where dist < 0.9;
    """.stripMargin

  def SelectAllByKeyWithDeleted(key: String, limit: String, offset: Int) =
    s"""
       |SELECT $FieldsWithoutTable from (SELECT $FieldsWithoutTable, email <-> '$key' AS dist
       |FROM users
       |ORDER BY dist LIMIT $limit OFFSET $offset) as sub  where dist < 0.9;
    """.stripMargin

  // TODO - finish that
  def SelectAllByKey(
    key: String,
    includeDeleted: Boolean,
    onlyDeleted: Boolean,
    includeStudents: Boolean,
    onlyStudents: Boolean,
    studentRole: Role,
    limit: String,
    offset: Int
  ) = {
    val roleClause = {
      if (includeStudents) ""
      else if (onlyStudents) {
        s"""
          |INNER JOIN users_roles AS ur
          |ON ur.user_id = sub.id
          |AND role_id = '${studentRole.id}'
        """.stripMargin
      }
      else {
        s"""
           |INNER JOIN users_roles AS ur
           |ON ur.user_id = sub.id
           |AND role_id != '${studentRole.id}'
        """.stripMargin
      }
    }

    val deletedClause = {
      if (includeDeleted) ""
      else if (onlyDeleted) "WHERE is_deleted = true"
      else "WHERE is_deleted = false"
    }

    s"""
       |SELECT ${FieldsWithTable("sub")}
       |FROM (
       |  SELECT *
       |  FROM $Table
       |  WHERE surname ILIKE '${key}%' OR givenname ILIKE '${key}%' OR email ILIKE '${key}%'
       |    OR surname ILIKE '%${key}' OR givenname ILIKE '%${key}' OR email ILIKE '%${key}'
       |    OR surname ILIKE '%${key}%' OR givenname ILIKE '%${key}%' OR email ILIKE '%${key}%'
       |) AS sub
       |$roleClause
       |$deletedClause
       |ORDER BY sub.givenname ASC LIMIT $limit OFFSET $offset
    """.stripMargin
  }

  def SelectOrgMembersByKey(param: String, organizationList: IndexedSeq[Organization]) = {
    val orgIdList = organizationList.map(org => s"'${org.id.toString}'").mkString(", ")

    s"""
       |SELECT ${FieldsWithTable("sub")}
       |FROM (
       |  SELECT *
       |  FROM $Table
       |  WHERE surname ILIKE '${param}%' OR givenname ILIKE '${param}%' OR email ILIKE '${param}%'
       |    OR surname ILIKE '%${param}' OR givenname ILIKE '%${param}' OR email ILIKE '%${param}'
       |    OR surname ILIKE '%${param}%' OR givenname ILIKE '%${param}%' OR email ILIKE '%${param}%'
       |) AS sub
       |INNER JOIN organization_members AS om
       |  ON om.member_email = sub.email
       |  AND om.organization_id IN ($orgIdList)
       |ORDER BY sub.givenname ASC LIMIT 10
    """.
      stripMargin
  }

  def SelectByTags(tags: IndexedSeq[(String, String)], distinct: Boolean): String = {
    var whereClause = ""
    var distinctClause = ""
    val length = tags.length

    tags.zipWithIndex.map {
      case ((tagName, tagLang), index) =>
        whereClause += s"""(tags.name='${tagName}' AND tags.lang='${tagLang}')"""
        if (index != (length - 1)) whereClause += " OR "
    }

    whereClause = {
      if (whereClause != "") "WHERE " + whereClause
      // If tagList is empty, then there should be unexisting condition
      else "WHERE false != false"
    }

    if (distinct) {
      distinctClause = s"HAVING COUNT(DISTINCT tags.name) = $length"
    }

    def query(whereClause: String) =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE id IN (
         |    SELECT user_id
         |    FROM user_tags
         |    JOIN tags
         |      ON user_tags.tag_id = tags.id
         |    ${whereClause}
         |    GROUP BY user_id
         |    ${distinctClause}
         |)
         |GROUP BY $Table.id
    """.stripMargin

    query(whereClause)
  }

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

  val SelectAllWithTeam =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table, teams_scorers
       |WHERE $Table.id = teams_scorers.scorer_id
       |  AND teams_scorers.team_id = ?
       |  AND is_deleted = FALSE
       |ORDER BY $OrderBy
    """.stripMargin

  val SelectAllWithTeacher =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table, users_courses, courses
       |WHERE $Table.id = users_courses.user_id
       |  AND users_courses.course_id = courses.id
       |  AND courses.teacher_id = ?
       |  AND $Table.is_deleted = FALSE
       |  AND courses.is_deleted = FALSE
       |ORDER BY $OrderBy
    """.stripMargin

  val SelectAllWithConversation =
    s"""
       |SELECT $FieldsWithoutHash
       |FROM $Table, users_conversations as uc
       |WHERE $Table.id = uc.user_id
       |  AND uc.conversation_id = ?
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
  override def list(userIds: IndexedSeq[UUID])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    serializedT(userIds)(find(_)).map(_.map { userList =>
      userList.map { user =>
        user.copy(hash = None)
      }
    })
  }

  /**
   * List all users who have a certain role.
   *
   * @param role
   * @param conn
   * @return
   */
  override def list(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAllWithRole, Seq[Any](role.id))
  }

  /**
   * List student users in a given group.
   *
   * @return a future disjunction containing either the student users, or a failure
   */
  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    cacheRepository.cacheSeqUser.getCached(cacheStudentsKey(course.id)).flatMap {
      case \/-(userList) => Future successful \/.right[RepositoryError.Fail, IndexedSeq[User]](userList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          userList <- lift(queryList(SelectAllWithCourse, Seq[Any](course.id)))
          _ <- lift(cacheRepository.cacheSeqUser.putCache(cacheStudentsKey(course.id))(userList, ttl))
        } yield userList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * List scorers on a given team.
   *
   * @return a future disjunction containing either the scorers, or a failure
   */
  override def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    val key = cacheTeamScorersKey(team.id)
    cacheRepository.cacheSeqUser.getCached(key).flatMap {
      case \/-(userList) => Future successful \/.right[RepositoryError.Fail, IndexedSeq[User]](userList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          userList <- lift(queryList(SelectAllWithTeam, Seq[Any](team.id)))
          _ <- lift(cacheRepository.cacheSeqUser.putCache(key)(userList, ttl))
        } yield userList
      case -\/(error) => Future successful -\/(error)
    }
  }

  override def list(conversation: Conversation)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAllWithConversation, Seq[Any](conversation.id))
  }

  /**
   * List students for a given teacher
   * @param teacher
   * @param conn
   * @return
   */
  override def list(teacher: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectAllWithTeacher, Seq[Any](teacher.id))
  }

  /**
   * List users by tags
   *
   * @param tags (tagName:String, tagLang:String)
   * @param distinct Boolean If true each user should have all listed tags,
   *                 if false user should have at least one listed tag
   */
  override def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    queryList(SelectByTags(tags, distinct))
  }

  /**
   * Find a user by ID.
   *
   * @param id the UUID of the user to search for.
   * @return a future disjunction containing either the user, or a failure
   */
  override def find(id: UUID, includeDeleted: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
    cacheRepository.cacheUser.getCached(cacheUserKey(id)).flatMap {
      case \/-(user) => Future successful \/.right[RepositoryError.Fail, User](user)
      case -\/(noResults: RepositoryError.NoResults) => {
        val query = {
          if (includeDeleted) SelectOneAdmin
          else SelectOne
        }
        for {
          user <- lift(queryOne(query, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheUsernameKey(user.username))(user.id, ttl))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheUsernameKey(user.email))(user.id, ttl))
          _ <- lift(cacheRepository.cacheUser.putCache(cacheUserKey(user.id))(user, ttl))
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
  override def find(identifier: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
    cacheRepository.cacheUUID.getCached(cacheUsernameKey(identifier)).flatMap {
      case \/-(userId) => find(userId)
      case -\/(noResults: RepositoryError.NoResults) => {
        for {
          user <- lift(queryOne(SelectOneByIdentifier, Seq[Any](identifier, identifier)))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheUsernameKey(user.username))(user.id, ttl))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheUsernameKey(user.email))(user.id, ttl))
          _ <- lift(cacheRepository.cacheUser.putCache(cacheUserKey(user.id))(user, ttl))
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
  override def findDeleted(identifier: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
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
  override def update(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
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
      _ <- lift(cacheRepository.cacheSeqUser.removeCached(cacheUserKey(updated.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheUsernameKey(updated.username)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheUsernameKey(updated.email)))
    } yield updated
  }

  /**
   * Mark user as deleted and update username and email.
   *
   * @param user the user to be deleted
   * @return a future disjunction containing either the deleted user, or a failure
   */
  override def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]] = {
    val timestamp = System.currentTimeMillis / 1000
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](
        (s"deleted_${timestamp}_" + user.username),
        (s"deleted_${timestamp}_" + user.email),
        user.id,
        user.version
      )))
      _ <- lift(cacheRepository.cacheSeqUser.removeCached(cacheUserKey(deleted.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheUsernameKey(deleted.username)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheUsernameKey(deleted.email)))
    } yield deleted
  }

  /**
   * Search by triagrams for autocomplete
   * @param key
   * @param conn
   */
  def triagramSearch(key: String, includeDeleted: Boolean, limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    val queryLimit = {
      if (limit == 0) "ALL"
      else limit.toString
    }

    val query = {
      if (includeDeleted) SelectAllByKeyWithDeleted(key, queryLimit, offset)
      else SelectAllByKeyNotDeleted(key, queryLimit, offset)
    }

    queryList(query)
  }

  def searchOrganizationMembers(key: String, organizationList: IndexedSeq[Organization])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    for {
      _ <- predicate(organizationList.nonEmpty)(RepositoryError.BadParam("core.UserRepositoryPostgres.searchOrganizationTeammate.org.empty"))
      result <- lift(queryList(SelectOrgMembersByKey(key, organizationList)))
    } yield result
  }
}
