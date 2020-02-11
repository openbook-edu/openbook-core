package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalaz.{\/}

trait CourseScheduleRepository extends Repository {
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseSchedule]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]

  def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]
  def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]
  def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]]
}
