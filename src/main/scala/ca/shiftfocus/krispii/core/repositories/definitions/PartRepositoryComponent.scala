package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait PartRepositoryComponent {
  /**
   * Value storing an instance of the repository. Should be overridden with
   * a concrete implementation to be used via dependency injection.
   */
  val partRepository: PartRepository

  trait PartRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list: Future[IndexedSeq[Part]]
    def list(project: Project): Future[IndexedSeq[Part]]
    def list(component: Component): Future[IndexedSeq[Part]]
    def listEnabled(project: Project, user: User): Future[IndexedSeq[Part]]
    def listEnabled(project: Project, course: Course): Future[IndexedSeq[Part]]
    def find(id: UUID): Future[Option[Part]]
    def find(project: Project, position: Int): Future[Option[Part]]
    def insert(part: Part)(implicit conn: Connection): Future[Part]
    def update(part: Part)(implicit conn: Connection): Future[Part]
    def delete(part: Part)(implicit conn: Connection): Future[Boolean]
    def delete(project: Project)(implicit conn: Connection): Future[Boolean]

    def reorder(project: Project, parts: IndexedSeq[Part])(implicit conn: Connection): Future[IndexedSeq[Part]]

    def isEnabled(part: Part, user: User): Future[Boolean]
    def isEnabled(part: Part, course: Course): Future[Boolean]
  }
}
