package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait TaskRepository extends Repository {

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]]
  def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]]
  def list(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]]
  //def list(project: Project, partNum: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def findNow(student: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def findNowFromAll(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def find(project: Project, part: Part, taskNum: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]

  def insert(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def update(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]]
  def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]]
}