package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class User(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    username: String,
    email: String,
    hash: Option[String] = None,
    givenname: String,
    surname: String,
    alias: Option[String] = None,
    roles: IndexedSeq[Role] = IndexedSeq.empty[Role],
    token: Option[UserToken] = None,
    accountType: String,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherUser: User => this.id == anotherUser.id
      case _ => false
    }
  }

  override def hashCode: Int = this.id.hashCode()

  override def toString: String = {
    s"""User(id: ${id.toString}, version: $version, username: $username, email: $email, full name: '$givenname $surname', alias: '${alias.getOrElse("")}')"""
  }
}

/**
 * User companion object. Fills the role of input/output mappers in the
 * layered architecture. This object should handle all interaction with the
 * technical services layer for Users.
 *
 * For individual user lookups, this mapper will check the in-memory cache
 * first, followed by the database.
 */
object User {
  implicit val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(
        "id" -> user.id.toString,
        "version" -> user.version,
        "username" -> user.username,
        "email" -> user.email,
        "givenname" -> user.givenname,
        "surname" -> user.surname,
        "alias" -> user.alias,
        "roles" -> user.roles,
        "accountType" -> user.accountType,
        "createdAt" -> user.createdAt,
        "updatedAt" -> user.updatedAt
      )
    }
  }
}

case class UserInfo(
  user: User,
  roles: IndexedSeq[Role],
  courses: IndexedSeq[Course]
)

object UserInfo {
  implicit val userInfoWrites = new Writes[UserInfo] {
    def writes(userInfo: UserInfo): JsValue = {
      Json.obj(
        "id" -> userInfo.user.id.toString,
        "version" -> userInfo.user.version,
        "username" -> userInfo.user.username,
        "email" -> userInfo.user.email,
        "givenname" -> userInfo.user.givenname,
        "surname" -> userInfo.user.surname,
        "alias" -> userInfo.user.alias,
        "accountType" -> userInfo.user.accountType,
        "createdAt" -> userInfo.user.createdAt,
        "updatedAt" -> userInfo.user.updatedAt
      ).deepMerge(Json.obj(
          "roles" -> userInfo.roles.map(_.name.toLowerCase()),
          "sections" -> userInfo.courses.map(_.name)
        ))
    }
  }
}
