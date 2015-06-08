package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import java.util.UUID
import scala.concurrent.Future
import scalaz.{ \/, EitherT }

trait TaskScratchpadRepository extends Repository {

  val documentRepository: DocumentRepository

  /**
   * The usual CRUD functions for the projects table.
   */
  def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]]
  def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]]

  def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]]

  def insert(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]]
  def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]]
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]]
}
