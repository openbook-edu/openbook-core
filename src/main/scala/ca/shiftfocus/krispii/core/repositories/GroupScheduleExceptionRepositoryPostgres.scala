package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Group
import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.{DateTime, LocalDate, LocalTime}
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GroupScheduleExceptionRepositoryPostgres(
  val userRepository: UserRepository,
  val groupScheduleRepository: GroupScheduleRepository,
  val cacheRepository: CacheRepository
)
    extends GroupScheduleExceptionRepository with PostgresRepository[GroupScheduleException] {

  override val entityName = "GroupScheduleException"

  def constructor(row: RowData): GroupScheduleException = {
    GroupScheduleException(
      row("id").asInstanceOf[UUID],
      row("user_id").asInstanceOf[UUID],
      row("group_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("start_date").asInstanceOf[DateTime],
      row("end_date").asInstanceOf[DateTime],
      row("reason").asInstanceOf[String],
      row("block").asInstanceOf[Boolean],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, user_id, group_id, start_date, end_date, reason, block"
  val Table = "schedule_exceptions"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET user_id = ?, group_id = ?, start_date = ?, end_date = ?, reason = ?, version = ?, updated_at = ?, block = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val SelectForGroup =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE group_id = ?
       |ORDER BY start_date asc, end_date asc
     """.stripMargin

  val SelectForUserAndGroup =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
       |  AND group_id = ?
       |ORDER BY id ASC
     """.stripMargin

  /**
   * Find all scheduling exceptions for one student in one group.
   *
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return a vector of the returned groups
   */
  override def list(user: User, group: Group) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupScheduleException]]] = { // format: ON
    cacheRepository.cacheSeqGroupScheduleException.getCached(cacheExceptionsKey(group.id, user.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForUserAndGroup, Array[Any](user.id, group.id)))
          _ <- lift(cacheRepository.cacheSeqGroupScheduleException.putCache(cacheExceptionsKey(group.id, user.id))(schedules, ttl))
        } yield schedules
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all schedule exceptions for a given group.
   */
  override def list(group: Group) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupScheduleException]]] = { // format: ON
    cacheRepository.cacheSeqGroupScheduleException.getCached(cacheExceptionsKey(group.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForGroup, Seq[Any](group.id)))
          _ <- lift(cacheRepository.cacheSeqGroupScheduleException.putCache(cacheExceptionsKey(group.id))(schedules, ttl))
        } yield schedules
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the UUID to search for
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return an optional task if one was found
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]] = {
    cacheRepository.cacheGroupScheduleException.getCached(cacheExceptionKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheGroupScheduleException.putCache(cacheExceptionKey(id))(schedule, ttl))
        } yield schedule
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Create a new group schedule exception.
   *
   * @param groupScheduleException The groupScheduleException to be inserted
   * @return the new group
   */
  override def insert(groupScheduleException: GroupScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]] = { // format: ON
    for {
      inserted <- lift(queryOne(Insert, Array(
        groupScheduleException.id,
        1L,
        new DateTime,
        new DateTime,
        groupScheduleException.userId,
        groupScheduleException.groupId,
        groupScheduleException.startDate,
        groupScheduleException.endDate,
        groupScheduleException.reason,
        groupScheduleException.block
      )))
      _ <- lift(cacheRepository.cacheGroupScheduleException.removeCached(cacheExceptionKey(inserted.id)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(inserted.groupId)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(inserted.groupId, inserted.userId)))
    } yield inserted
  }

  /**
   * Update a group.
   *
   * @param groupScheduleException The groupScheduleException to be updated.
   * @return the updated group
   */
  override def update(groupScheduleException: GroupScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]] = { // format: ON
    for {
      updated <- lift(queryOne(Update, Array(
        groupScheduleException.userId,
        groupScheduleException.groupId,
        groupScheduleException.startDate,
        groupScheduleException.endDate,
        groupScheduleException.reason,
        groupScheduleException.version + 1,
        new DateTime,
        groupScheduleException.block,
        groupScheduleException.id,
        groupScheduleException.version
      )))
      _ <- lift(cacheRepository.cacheGroupScheduleException.removeCached(cacheExceptionKey(updated.id)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(updated.groupId)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(updated.groupId, updated.userId)))
    } yield updated
  }

  /**
   * Delete a group.
   *
   * @param groupScheduleException The groupScheduleException to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(groupScheduleException: GroupScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]] = { // format: ON
    for {
      deleted <- lift(queryOne(Delete, Array(groupScheduleException.id, groupScheduleException.version)))
      _ <- lift(cacheRepository.cacheGroupScheduleException.removeCached(cacheExceptionKey(deleted.id)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(deleted.groupId)))
      _ <- lift(cacheRepository.cacheSeqGroupScheduleException.removeCached(cacheExceptionsKey(deleted.groupId, deleted.userId)))
    } yield deleted
  }
}
