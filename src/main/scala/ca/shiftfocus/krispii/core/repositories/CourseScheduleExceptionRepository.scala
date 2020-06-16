package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.course.Course

import scala.concurrent.Future
import scalaz.\/

trait CourseScheduleExceptionRepository extends Repository {
  val userRepository: UserRepository
  val courseScheduleRepository: CourseScheduleRepository

  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]]
  def list(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]]

  def insert(courseSchedule: CourseScheduleException) // format: OFF
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]]

  def update(courseSchedule: CourseScheduleException)
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]]

  def delete(courseSchedule: CourseScheduleException)
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] // format: ON
}
