package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work.{Work, LongAnswerWork}
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.ot._
import concurrent.Future

trait DocumentRepositoryComponent {
  val workRepository: DocumentRepository

  trait DocumentRepository {

    /**
     * Fetch all documents that belong to a user.
     *
     * @param user
     * @param project
     * @return
     */
    def list(user: User): Future[IndexedSeq[LongAnswerWork]]

    /**
     * Fetch revision history for a document.
     *
     * @param user
     * @param task
     * @param fromVer
     * @param toVer
     */
    def list(user: User, task: Task, fromVer: Option[Long] = None, toVer: Option[Long] = None)

    def find(user: User, task: Task): Future[Option[LongAnswerWork]]

    /**
     * Create a new long-answer work.
     *
     * This method "sets up" the new work in the database, but it does not actually insert the first bit of content!
     * After "creating" a work, use update to start editing it.
     *
     * @param user
     * @param task
     * @return
     */
    def create(user: User, task: Task): Future[Option[LongAnswerWork]]

    /**
     * Update a long-answer work
     *
     * @param user
     * @param task
     * @param version
     * @param operations
     * @return
     */
    def update(user: User, task: Task, version: Long, operations: IndexedSeq[Operation]): Future[Option[(Long)]]

    /**
     * Delete a work.
     *
     * This, obviously, deletes work. It should delete both the work itself and its entire revision history.
     *
     * @param user
     * @param task
     * @return
     */
    def delete(user: User, task: Task): Future[Boolean]

  }
}