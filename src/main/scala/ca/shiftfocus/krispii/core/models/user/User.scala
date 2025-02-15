package ca.shiftfocus.krispii.core.models.user

import java.util.UUID

import ca.shiftfocus.krispii.core.models.group.Course
import ca.shiftfocus.krispii.core.models.{Role, Tag, UserToken}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._

trait UserTrait {
  val id: UUID
  val version: Long
  val username: String
  val email: String
  val hash: Option[String]
  val givenname: String
  val surname: String
  val alias: Option[String]
  val roles: IndexedSeq[Role]
  val tags: IndexedSeq[Tag]
  val token: Option[UserToken]
  val accountType: String
  // We should show this field only to admins, that's why it is not included in Json writes, and is included in Json adminWrites
  val isDeleted: Boolean
  val createdAt: DateTime
  val updatedAt: DateTime

  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherUser: UserTrait => this.id == anotherUser.id
      case _ => false
    }
  }

  override def hashCode: Int = this.id.hashCode()

  override def toString: String = {
    s"""User(id: ${id.toString}, version: $version, username: $username, email: $email, full name: '$givenname $surname', alias: '${alias.getOrElse("")}', accountType: '${accountType}')"""
  }
}

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
  tags: IndexedSeq[Tag] = IndexedSeq.empty[Tag],
  token: Option[UserToken] = None,
  accountType: String,
  // We should show this field only to admins, that's why it is not included in Json writes, and is included in Json adminWrites
  isDeleted: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends UserTrait
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
    /* hard to write this in a functional way because we don't want to write
       hash nor token, and in userWrites we don't want to write isDeleted.*/
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
        "tags" -> user.tags,
        "accountType" -> user.accountType,
        "createdAt" -> user.createdAt,
        "updatedAt" -> user.updatedAt
      )
    }

    def adminWrites(user: User): JsValue = {
      Json.obj(
        "id" -> user.id.toString,
        "version" -> user.version,
        "username" -> user.username,
        "email" -> user.email,
        "givenname" -> user.givenname,
        "surname" -> user.surname,
        "alias" -> user.alias,
        "roles" -> user.roles,
        "tags" -> user.tags,
        "accountType" -> user.accountType,
        "isDeleted" -> user.isDeleted,
        "createdAt" -> user.createdAt,
        "updatedAt" -> user.updatedAt
      )
    }
  }

  implicit val userReads: Reads[User] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "username").read[String] and
    (__ \ "email").read[String] and
    (__ \ "hash").readNullable[String] and
    (__ \ "givenname").read[String] and
    (__ \ "surname").read[String] and
    (__ \ "alias").readNullable[String] and
    (__ \ "roles").read[IndexedSeq[Role]] and
    (__ \ "tags").read[IndexedSeq[Tag]] and
    (__ \ "token").readNullable[UserToken] and
    (__ \ "accountType").read[String] and
    (__ \ "isDeleted").read[Boolean] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(User.apply _)
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
        "tags" -> userInfo.user.tags,
        "accountType" -> userInfo.user.accountType,
        "createdAt" -> userInfo.user.createdAt,
        "updatedAt" -> userInfo.user.updatedAt
      ).deepMerge(Json.obj(
          "roles" -> userInfo.roles.map(_.name.toLowerCase()),
          "sections" -> userInfo.courses.map(_.name)
        ))
    }
  }

  /*implicit val userInfoReads = new Reads[UserInfo] {
    def reads(userInfo: UserInfo): JsValue = {
      Json.obj(
        "id" -> userInfo.user.id.toString,
        "version" -> userInfo.user.version,
        "username" -> userInfo.user.username,
        "email" -> userInfo.user.email,
        "givenname" -> userInfo.user.givenname,
        "surname" -> userInfo.user.surname,
        "alias" -> userInfo.user.alias,
        "tags" -> userInfo.user.tags,
        "accountType" -> userInfo.user.accountType,
        "createdAt" -> userInfo.user.createdAt,
        "updatedAt" -> userInfo.user.updatedAt
      ).deepMerge(Json.obj(
        "roles" -> userInfo.roles.map(_.name.toLowerCase()),
        "sections" -> userInfo.courses.map(_.name)
      ))
    }
  }*/
}
