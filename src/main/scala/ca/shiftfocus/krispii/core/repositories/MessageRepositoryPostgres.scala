package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.{ Conversation, Message }
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scalaz.\/

/**
 * Work with database tables: users_subscriptions, stripe_events
 */
class MessageRepositoryPostgres extends MessageRepository with PostgresRepository[Message] {
  override val entityName = "Message"
  override def constructor(row: RowData): Message = {
    Message(
      row("id").asInstanceOf[UUID],
      row("conversation_id").asInstanceOf[UUID],
      row("user_id").asInstanceOf[UUID],
      row("content").asInstanceOf[String],
      Option(row("revision_id")).map(_.asInstanceOf[String]),
      Option(row("revision_type")).map(_.asInstanceOf[String]),
      Option(row("revision_version")).map(_.asInstanceOf[String]),
      row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "messages"
  val Fields = "id, conversation_id, user_id, content, revision_id, revision_type, revision_version, created_at"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  def SelectRangeByConversationId(conversationId: UUID, limit: String, offset: Int) =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE conversation_id = '$conversationId'
       |ORDER BY created_at ASC
       |LIMIT $limit OFFSET $offset
  """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]] = {
    queryOne(SelectOne, Seq[Any](id))
  }

  def list(conversationId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]] = {
    val queryLimit = {
      if (limit == 0) "ALL"
      else limit.toString
    }

    queryList(SelectRangeByConversationId(conversationId, queryLimit, offset))
  }

  def insert(message: Message)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]] = {
    val params = Seq[Any](
      message.id, message.conversationId, message.userId, message.content, message.revisionId,
      message.revisionType, message.revisionVersion, message.createdAt
    )

    queryOne(Insert, params)
  }

  def delete(message: Message)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]] = {
    queryOne(Delete, Seq[Any](message.id))
  }
}
