package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.course.Exam
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait ExamRepository extends Repository {
  // val userRepository: UserRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]]
  def list(coordinator: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]]

  def find(examId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]]
  def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]]

  def insert(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]]
  def update(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]]
  def delete(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]]
  // don't need to have addTeam, addTest because they refer back with examId
}
