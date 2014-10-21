package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib._
import com.shiftfocus.krispii.common.models._
import scala.concurrent.Future

trait TaskResponseRepositoryComponent {
  /**
   * Value storing an instance of the repository. Should be overridden with
   * a concrete implementation to be used via dependency injection.
   */
  val taskResponseRepository: TaskResponseRepository

  trait TaskResponseRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    //def list(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User, task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def find(user: User, task: Task)(implicit conn: Connection): Future[Option[TaskResponse]]
    def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[Option[TaskResponse]]
    def insert(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse]
    def update(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse]
    def delete(taskResponse: TaskResponse)(implicit conn: Connection): Future[Boolean]
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
    def forceComplete(task: Task, section: Section)(implicit conn: Connection): Future[Boolean]
  }
}
