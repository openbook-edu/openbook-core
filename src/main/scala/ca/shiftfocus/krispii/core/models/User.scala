package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class User(
  id: UUID = UUID.random,
  version: Long = 0,
  username: String,
  email: String,
  password: Option[String] = None,
  passwordHash: Option[String] = None,
  givenname: String,
  surname: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) {
  /**
   * Indicates whether another user is equal to this one.
   *
   * Two users are considered to be the same if they share the same UUID.
   *
   * @param that
   * @return
   */
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherUser: User => this.id == anotherUser.id
      case _ => false
    }
  }

  override def toString() = {
    s"User(id: ${id.string}, version: $version, username: $username, email: $email, full name: '$givenname $surname')"
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
        "id" -> userInfo.user.id.string,
        "version" -> userInfo.user.version,
        "username" -> userInfo.user.username,
        "email" -> userInfo.user.email,
        "givenname" -> userInfo.user.givenname,
        "surname" -> userInfo.user.surname
      ).deepMerge(Json.obj(
        "roles" -> userInfo.roles.map(_.name.toLowerCase()),
        "sections" -> userInfo.courses.map(_.name)
      ))
    }
  }
}
case class EmailAndUsernameAlreadyExistException(msg: String) extends Exception
case class EmailAlreadyExistsException(msg: String) extends Exception
case class UsernameAlreadyExistsException(msg: String) extends Exception



case class UserPostTest(
  username: Option[String],
  email: Option[String],
  password: Option[String],
  givenname: Option[String],
  surname: Option[String]
)
object UserPostTest {
  implicit val userPostReads = (
    (__ \ "username").readNullable[String] and
    (__ \ "email").readNullable[String] and
    (__ \ "password").readNullable[String] and
    (__ \ "givenname").readNullable[String] and
    (__ \ "surname").readNullable[String]
  )(UserPostTest.apply _)
}

case class UserPut(
  id: UUID,
  version: Long,
  username: Option[String] = None,
  email: Option[String] = None,
  password: Option[String] = None,
  givenname: Option[String] = None,
  surname: Option[String] = None
)
object UserPut {
  implicit val userPutReads = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "username").readNullable[String] and
    (__ \ "email").readNullable[String] and
    (__ \ "password").readNullable[String] and
    (__ \ "givenname").readNullable[String] and
    (__ \ "surname").readNullable[String]
  )(UserPut.apply _)
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

  implicit val userReads = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "username").read[String] and
    (__ \ "email").read[String] and
    (__ \ "password").readNullable[String].filter({
      case Some(p) => p.nonEmpty
      case None => true
    }) and
    (__ \ "passwordHash").readNullable[String].filter({
      case Some(p) => p.nonEmpty
      case None => true
    }) and
    (__ \ "givenname").read[String] and
    (__ \ "surname").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(User.apply(_: UUID, _: Long, _: String, _: String, _: Option[String], _: Option[String],
               _: String, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(
        "id" -> user.id.string,
        "version" -> user.version,
        "username" -> user.username,
        "email" -> user.email,
        "givenname" -> user.givenname,
        "surname" -> user.surname
      )
    }
  }

  def apply(row: RowData): User =  {
    User(
      id           = UUID(row("id").asInstanceOf[Array[Byte]]),
      version      = row("version").asInstanceOf[Long],
      email        = row("email").asInstanceOf[String],
      username     = row("username").asInstanceOf[String],
      password     = None,
      passwordHash = Option(row("password_hash")) match {
        case Some(cell) => Some(cell.asInstanceOf[String])
        case None => None
      },
      givenname    = row("givenname").asInstanceOf[String],
      surname      = row("surname").asInstanceOf[String],
      createdAt    = Option(row("created_at").asInstanceOf[DateTime]),
      updatedAt    = Option(row("updated_at").asInstanceOf[DateTime])
    )
  }
}
