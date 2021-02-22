package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Group
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.{DateTime, LocalDate, LocalTime}
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GroupScheduleRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends GroupScheduleRepository with PostgresRepository[GroupSchedule] {

  override val entityName = "GroupSchedule"

  def constructor(row: RowData): GroupSchedule = {
    GroupSchedule(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("group_id").asInstanceOf[UUID],
      row("start_date").asInstanceOf[DateTime],
      row("end_date").asInstanceOf[DateTime],
      row("description").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, group_id, start_date, end_date, description"
  val Table = "schedules"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = "start_date ASC"

  val SelectAll = s"""
    |SELECT $Fields
    |FROM $Table
    |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE id = ?
  """.stripMargin

  val Insert = {
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin
  }

  val Update = {
    s"""
      |UPDATE $Table
      |SET group_id = ?, start_date = ?, end_date = ?, description = ?, version = ?, updated_at = ?
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $Fields
    """.stripMargin
  }

  val Delete =
    s"""
     |DELETE FROM $Table
     |WHERE id = ?
     |  AND version = ?
     |RETURNING $Fields
   """.stripMargin

  val SelectByGroupId =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE group_id = ?
      |ORDER BY $OrderBy, start_date ASC, end_date ASC
    """.stripMargin

  /**
   * List all schedules for a given group
   */
  override def list(group: Group)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupSchedule]]] = {
    cacheRepository.cacheSeqGroupSchedule.getCached(cacheSchedulesKey(group.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectByGroupId, Seq[Any](group.id)))
          _ <- lift(cacheRepository.cacheSeqGroupSchedule.putCache(cacheSchedulesKey(group.id))(schedules, ttl))
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]] = {
    cacheRepository.cacheGroupSchedule.getCached(cacheScheduleKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheGroupSchedule.putCache(cacheScheduleKey(id))(schedule, ttl))
        } yield schedule
      case -\/(error) => Future successful -\/(error)
    }

  }

  /**
   * Create a new schedule.
   *
   * @param groupSchedule The group to be inserted
   * @return the new group
   */
  override def insert(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]] = {
    for {
      newSchedule <- lift(queryOne(Insert, Seq[Any](
        groupSchedule.id,
        1L,
        new DateTime,
        new DateTime,
        groupSchedule.groupId,
        groupSchedule.startDate,
        groupSchedule.endDate,
        groupSchedule.description
      )))
      _ <- lift(cacheRepository.cacheGroupSchedule.removeCached(cacheScheduleKey(newSchedule.id)))
      _ <- lift(cacheRepository.cacheSeqGroupSchedule.removeCached(cacheSchedulesKey(newSchedule.groupId)))
    } yield newSchedule
  }

  /**
   * Update a schedule.
   *
   * @param groupSchedule The groupSchedule to be updated.
   * @return the updated group
   */
  override def update(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]] = {
    for {
      updated <- lift(queryOne(Update, Seq[Any](
        groupSchedule.groupId,
        groupSchedule.startDate,
        groupSchedule.endDate,
        groupSchedule.description,
        groupSchedule.version + 1,
        new DateTime,
        groupSchedule.id,
        groupSchedule.version
      )))
      _ <- lift(cacheRepository.cacheGroupSchedule.removeCached(cacheScheduleKey(updated.id)))
      _ <- lift(cacheRepository.cacheSeqGroupSchedule.removeCached(cacheSchedulesKey(updated.groupId)))
    } yield updated
  }

  /**
   * Delete a schedule.
   *
   * @param groupSchedule The group to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](groupSchedule.id, groupSchedule.version)))
      _ <- lift(cacheRepository.cacheGroupSchedule.removeCached(cacheScheduleKey(deleted.id)))
      _ <- lift(cacheRepository.cacheSeqGroupSchedule.removeCached(cacheSchedulesKey(deleted.groupId)))
    } yield deleted
  }
}
