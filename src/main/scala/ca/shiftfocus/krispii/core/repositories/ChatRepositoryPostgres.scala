package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.{Chat, User}
import java.util.UUID

import ca.shiftfocus.krispii.core.models.course.Course
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class ChatRepositoryPostgres extends ChatRepository with PostgresRepository[Chat] {

  override val entityName = "Chat"

  /**
   * Instantiate a Document given a row result from the database. Must be provided
   * with the owner and users.
   *
   * @param row
   * @return
   */
  def constructor(row: RowData): Chat = {
    Chat(
      courseId = row("course_id").asInstanceOf[UUID],
      messageNum = row("message_num").asInstanceOf[Long],
      userId = row("user_id").asInstanceOf[UUID],
      message = row("message").asInstanceOf[String],
      hidden = row("hidden").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "chat_logs"
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
       |ORDER BY $OrderBy
       |LIMIT ?
       |OFFSET ?
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
       |ORDER BY $OrderBy
       |LIMIT ?
       |OFFSET ?
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
       |VALUES (?, (SELECT coalesce(MAX(cl.message_num), 0)+1 FROM chat_logs AS cl WHERE cl.course_id = ?), ?, ?, ?, ?)
       |RETURNING $Fields
     """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET hidden = ?
       |WHERE course_id = ?
       |  AND message_num = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete = s"""
    |DELETE FROM $Table
    |WHERE course_id = ?
    |  AND message_num = ?
    |RETURNING $Fields
    """.stripMargin
  /**
   * List all chat logs for a course
   *
   * @param course
   * @param conn
   * @return
   */
  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectAllByCourse, Seq[Any](course.id))
  }

  /**
   * List only an indicated portion of chat logs for a course
   *
   * @param course
   * @param num
   * @param offset
   * @param conn
   * @return
   */
  override def list(course: Course, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectOffsetByCourse, Seq[Any](course.id, num, offset))
  }

  /**
   * List all chat logs for a course for a user
   *
   * @param course
   * @param user
   * @param conn
   * @return
   */
  override def list(course: Course, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectAllByCourseAndUser, Seq[Any](course.id, user.id))
  }

  /**
   * List only an indicated portion of chat logs for a course for a user
   *
   * @param course
   * @param user
   * @param num
   * @param offset
   * @param conn
   * @return
   */
  override def list(course: Course, user: User, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]] = {
    queryList(SelectOffsetByCourseAndUser, Seq[Any](course.id, user.id, num, offset))
  }

  /**
   * Find a chat log for a course by number
   *
   * @param course
   * @param messageNum
   * @param conn
   * @return
   */
  override def find(course: Course, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(SelectOne, Seq[Any](course.id, messageNum))
  }

  /**
   * Create a new chat log
   *
   * @param chat
   * @param conn
   * @return
   */
  override def insert(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(Insert, Seq[Any](chat.courseId, chat.courseId, chat.userId, chat.message, false, new DateTime))
  }

  /**
   * Update a chat message
   *
   * @param chat
   * @param conn
   * @return
   */
  override def update(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(Update, Seq[Any](chat.hidden, chat.courseId, chat.messageNum))
  }

  def delete(courseId: UUID, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]] = {
    queryOne(Delete, Seq[Any](courseId, messageNum))
  }

}
