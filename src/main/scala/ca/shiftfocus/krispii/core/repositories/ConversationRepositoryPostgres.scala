package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.{ Conversation, User }
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

/**
 * Work with database tables: users_subscriptions, stripe_events
 */
class ConversationRepositoryPostgres(
    val userRepository: UserRepository
) extends ConversationRepository with PostgresRepository[Conversation] {

  override val entityName = "Conversation"
  override def constructor(row: RowData): Conversation = {
    Conversation(
      row("id").asInstanceOf[UUID],
      row("owner_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("title").asInstanceOf[String],
      row("shared").asInstanceOf[Boolean],
      Option(row("entity_id")).map(_.asInstanceOf[UUID]),
      Option(row("entity_type")).map(_.asInstanceOf[String]),
      IndexedSeq.empty[User],
      row("is_deleted").asInstanceOf[Boolean],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "conversations"
  val Fields = "id, owner_id, version, title, shared, entity_id, entity_type, is_deleted, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.created_at ASC"

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       | AND is_deleted = false
     """.stripMargin

  def SelectRange(limit: String, offset: Int) =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE is_deleted = false
       |ORDER BY $OrderBy
       |LIMIT $limit OFFSET $offset
  """.stripMargin

  def SelectRangeByEntityId(entityId: UUID, limit: String, offset: Int) =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE entity_id = '$entityId'
       |  AND is_deleted = false
       |ORDER BY $OrderBy
       |LIMIT $limit OFFSET $offset
  """.stripMargin

  def SelectRangeByEntityAndUserId(entityId: UUID, userId: UUID, limit: String, offset: Int) =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table
       |JOIN users_conversations AS uc
       |  ON uc.conversation_id = $Table.id
       |  AND uc.user_id = '$userId'
       |WHERE ${Table}.entity_id = '$entityId'
       |  AND is_deleted = false
       |ORDER BY $OrderBy
       |LIMIT $limit OFFSET $offset
  """.stripMargin

  val SelectByEntityIdAfter =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE entity_id = ?
       |  AND created_at > ?
       |  AND is_deleted = false
       |ORDER BY $OrderBy
  """.stripMargin

  val SelectByEntityAndUserIdAfter =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table
       |JOIN users_conversations AS uc
       |  ON uc.conversation_id = $Table.id
       |  AND uc.user_id = ?
       |WHERE ${Table}.entity_id = ?
       |  AND ${Table}.created_at > ?
       |  AND is_deleted = false
       |ORDER BY $OrderBy
  """.stripMargin

  val AddMember =
    s"""
       |INSERT INTO users_conversations (conversation_id, user_id, created_at)
       |VALUES (?, ?, ?)
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |UPDATE $Table
       |SET is_deleted = true
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Conversation]] = {
    for {
      conversation <- lift(queryOne(SelectOne, Seq[Any](id)))
      members <- lift(userRepository.list(conversation))
    } yield conversation.copy(members = members)
  }

  def list(entityId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]] = {
    val queryLimit = {
      if (limit == 0) "ALL"
      else limit.toString
    }

    for {
      conversationList <- lift(queryList(SelectRangeByEntityId(entityId, queryLimit, offset)))
      conversationsWithMembers <- liftSeq(conversationList.map { conversation =>
        (for {
          members <- lift(userRepository.list(conversation))
          result = conversation.copy(members = members)
        } yield result).run
      })
    } yield conversationsWithMembers
  }

  def list(entityId: UUID, afterDate: DateTime)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]] = {
    for {
      conversationList <- lift(queryList(SelectByEntityIdAfter, Seq[Any](entityId, afterDate)))
      conversationsWithMembers <- liftSeq(conversationList.map { conversation =>
        (for {
          members <- lift(userRepository.list(conversation))
          result = conversation.copy(members = members)
        } yield result).run
      })
    } yield conversationsWithMembers
  }

  def listByUser(entityId: UUID, userId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]] = {
    val queryLimit = {
      if (limit == 0) "ALL"
      else limit.toString
    }

    for {
      conversationList <- lift(queryList(SelectRangeByEntityAndUserId(entityId, userId, queryLimit, offset)))
      conversationsWithMembers <- liftSeq(conversationList.map { conversation =>
        (for {
          members <- lift(userRepository.list(conversation))
          result = conversation.copy(members = members)
        } yield result).run
      })
    } yield conversationsWithMembers
  }

  def listByUser(entityId: UUID, userId: UUID, afterDate: DateTime)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]] = {
    for {
      conversationList <- lift(queryList(SelectByEntityAndUserIdAfter, Seq[Any](userId, entityId, afterDate)))
      conversationsWithMembers <- liftSeq(conversationList.map { conversation =>
        (for {
          members <- lift(userRepository.list(conversation))
          result = conversation.copy(members = members)
        } yield result).run
      })
    } yield conversationsWithMembers
  }

  def addMember(conversation: Conversation, newMember: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Conversation]] = {
    val params = Seq[Any](conversation.id, newMember.id, new DateTime)

    for {
      _ <- lift(queryNumRows(AddMember, params)(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
        case -\/(error) => -\/(error)
      })
      members <- lift(userRepository.list(conversation))
    } yield conversation.copy(members = members :+ newMember)
  }

  def insert(conversation: Conversation)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Conversation]] = {
    val params = Seq[Any](
      conversation.id, conversation.ownerId, conversation.version, conversation.title, conversation.shared,
      conversation.entityId, conversation.entityType, conversation.isDeleted, conversation.createdAt, conversation.updatedAt
    )

    queryOne(Insert, params)
  }

  def delete(conversation: Conversation)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Conversation]] = {
    for {
      deletedConversation <- lift(queryOne(Delete, Seq[Any](conversation.id, conversation.version)))
      members = conversation.members
    } yield deletedConversation.copy(members = members)
  }
}
