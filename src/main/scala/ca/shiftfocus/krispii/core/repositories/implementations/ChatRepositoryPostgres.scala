package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.{Course, Chat, User}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{RowData, ResultSet, Connection}
import play.api.libs.json.Json
import ws.kahn.ot.exceptions.IncompatibleDeltasException
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import play.api.Logger
import ws.kahn.ot.{InsertText, Delta}

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class ChatRepositoryPostgres extends ChatRepository with PostgresRepository[Chat] {

  /**
   * Instantiate a Document given a row result from the database. Must be provided
   * with the owner and users.
   *
   * @param row
   * @return
   */
  def constructor(row: RowData): Chat = {
    Chat(
      courseId = UUID(row("course_id").asInstanceOf[Array[Byte]]),
      messageNum = row("message_num").asInstanceOf[Long],
      userId = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      message = row("message").asInstanceOf[String],
      hidden = row("hidden").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table  = "chat_logs"
  val Fields = "course_id, message_num, user_id, message, hidden, created_at"
  val QMarks = "?, ?, ?, ?, ?, ?"
  val OrderBy = "message_num ASC"

  val SelectAllByCourse =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOffsetByCourse =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |LIMIT ? OFFSET ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectAllByCourseAndUser =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |  AND user_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOffsetByCourseAndUser =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |  AND user_id = ?
       |LIMIT ? OFFSET ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |  AND message_num = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, (SELECT MAX(message_num)+1 WHERE course_id = ?), ?, ?, ?, ?)
       |RETURNING $Fields
     """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET hidden = ?
       |WHERE course_id = ?
       |  AND message_num = ?
     """.stripMargin

  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectAllByCourse, Seq[Any](course.id.bytes))
  }
  override def list(course: Course, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectOffsetByCourse, Seq[Any](course.id.bytes, num, offset))
  }

  override def list(course: Course, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectAllByCourseAndUser, Seq[Any](course.id.bytes, user.id.bytes))
  }
  override def list(course: Course, user: User,  num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectAllByCourse, Seq[Any](course.id.bytes, user.id.bytes, num, offset))
  }

  override def find(course: Course, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(SelectOne, Seq[Any](course.id.bytes, messageNum))
  }

  override def insert(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(SelectOne, Seq[Any](chat.courseId.bytes, chat.courseId.bytes, chat.userId.bytes, chat.message, false, chat.createdAt))
  }

  override def update(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(SelectOne, Seq[Any](chat.hidden, chat.courseId.bytes, chat.messageNum))
  }
}