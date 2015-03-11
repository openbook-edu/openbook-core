package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait CourseScheduleRepository extends Repository {
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseSchedule]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]

  def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]
  def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]
  def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]

  def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]]
  def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]]
}