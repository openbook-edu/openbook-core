package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{EitherT, \/}

trait CourseScheduleExceptionRepository extends Repository {
  val userRepository: UserRepository
  val courseScheduleRepository: CourseScheduleRepository

  def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]]
  def list(user: User, course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]]

  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseScheduleException]]

  def insert(courseSchedule: CourseScheduleException)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseScheduleException]]
  def update(courseSchedule: CourseScheduleException)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseScheduleException]]
  def delete(courseSchedule: CourseScheduleException)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseScheduleException]]
}