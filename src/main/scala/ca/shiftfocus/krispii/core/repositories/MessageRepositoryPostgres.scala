package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{Message}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

/**
 * Work with database table messages
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
  def FieldsWithTable(table: String) = Fields.split(", ").map({ field => s"${table}." + field }).mkString(", ")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.created_at ASC"

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
       |ORDER BY $OrderBy
       |LIMIT $limit OFFSET $offset
  """.stripMargin

  def SelectByConversationIdAfter =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE conversation_id = ?
       | AND created_at > ?
       |ORDER BY $OrderBy
  """.stripMargin

  // If we have an entry in last_read_message for conversation, then we return messages that were created after read_at,
  // if there is nothing in last_read_message for conversation, then we return all messages
  def SelectNew =
    s"""
       |WITH
       |c AS (
       |  SELECT *
       |  FROM conversations
       |  INNER JOIN users_conversations AS uc
       |    ON uc.conversation_id = conversations.id
       |    AND uc.user_id = ?
       |  WHERE conversations.entity_id = ?
       |  AND conversations.is_deleted = FALSE),
       |
       |new_messages AS (
       |  SELECT ${FieldsWithTable(Table)}
       |  FROM $Table, c
       |  WHERE
       |    CASE
       |      WHEN (SELECT read_at FROM last_read_message AS lrm WHERE lrm.conversation_id = $Table.conversation_id AND lrm.user_id = ?) IS NOT NULL
       |        AND (SELECT read_at FROM last_read_message AS lrm WHERE lrm.conversation_id = $Table.conversation_id AND lrm.user_id = ?) < $Table.created_at THEN true
       |      WHEN (SELECT read_at FROM last_read_message AS lrm WHERE lrm.conversation_id = $Table.conversation_id AND lrm.user_id = ?) IS NULL THEN true
       |      ELSE false
       |    END
       |    AND $Table.conversation_id = c.id
       |    AND $Table.user_id != ?
       |)
       |
       |SELECT ${FieldsWithTable("new_messages")} FROM new_messages
  """.stripMargin

  def HasNew =
    s"""
       |SELECT count(*) AS messages_count
       |FROM ($SelectNew) AS subquery
  """.stripMargin

  val SelectLastRead =
    s"""
       |SELECT ${FieldsWithTable(Table)}
       |FROM $Table
       |INNER JOIN last_read_message AS lrm
       | ON lrm.conversation_id = $Table.conversation_id
       | AND lrm.user_id = ?
       | AND $Table.id = lrm.message_id
       |WHERE $Table.conversation_id = ?
     """.stripMargin

  val InsertLastRead =
    """
       |INSERT INTO last_read_message (conversation_id, user_id, message_id, read_at)
       |VALUES (?, ?, ?, ?)
     """.stripMargin

  val UpdateLastRead =
    """
       |UPDATE last_read_message
       |SET message_id = ?,
       |    read_at = ?
       |WHERE conversation_id = ?
       |  AND user_id = ?
       |RETURNING message_id
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

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]] = {
    queryOne(SelectOne, Seq[Any](id))
  }

  def list(conversationId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]] = {
    val queryLimit = {
      if (limit == 0) "ALL"
      else limit.toString
    }

    queryList(SelectRangeByConversationId(conversationId, queryLimit, offset))
  }

  def list(conversationId: UUID, afterDate: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]] = {
    queryList(SelectByConversationIdAfter, Seq[Any](conversationId, afterDate))
  }

  // TODO - add to redis
  def listNew(entityId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]] = {
    queryList(SelectNew, Seq[Any](userId, entityId, userId, userId, userId, userId))
  }

  // TODO - add to redis
  def hasNew(entityId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]] = {
    conn.sendPreparedStatement(HasNew, Array[Any](userId, entityId, userId, userId, userId, userId)).map { result =>
      result.rows match {
        case Some(resultSet) => resultSet.headOption match {
          case Some(firstRow) => {
            if (firstRow("messages_count").asInstanceOf[Long] > 0) {
              \/-(true)
            }
            else {
              \/-(false)
            }
          }
          case _ => \/-(false)
        }
        case None => \/-(false)
      }
    }.recover {
      case exception: Throwable => throw exception
    }
  }

  def setLastRead(conversationId: UUID, userId: UUID, messageId: UUID, readAt: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    conn.sendPreparedStatement(UpdateLastRead, Seq[Any](messageId, readAt, conversationId, userId)).flatMap { result =>
      result.rows match {
        case Some(resultSet) => resultSet.headOption match {
          case Some(firstRow) => Future successful \/-((): Unit)
          case None => {
            conn.sendPreparedStatement(InsertLastRead, Seq[Any](conversationId, userId, messageId, readAt)).map { result =>
              result.rows match {
                case Some(resultSet) => \/-((): Unit)
                case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not insert last read message"))
              }
            }.recover {
              case exception: Throwable => throw exception
            }
          }
        }
        case None => Future successful -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not update last read message"))
      }
    }.recover {
      case exception: Throwable => throw exception
    }
  }

  // TODO - add to redis
  def getLastRead(conversationId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]] = {
    queryOne(SelectLastRead, Seq[Any](userId, conversationId))
  }

  def insert(message: Message)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]] = {
    val params = Seq[Any](
      message.id, message.conversationId, message.userId, message.content, message.revisionId,
      message.revisionType, message.revisionVersion, message.createdAt
    )

    queryOne(Insert, params)
  }

  def delete(message: Message)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]] = {
    queryOne(Delete, Seq[Any](message.id))
  }
}
