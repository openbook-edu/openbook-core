package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait PartRepositoryComponent {
  self: ProjectRepositoryComponent =>

  val partRepository: PartRepository

  trait PartRepository {
    def list: Future[\/[RepositoryError, IndexedSeq[Part]]]
    def list(project: Project): Future[\/[RepositoryError, IndexedSeq[Part]]]
    def list(component: Component): Future[\/[RepositoryError, IndexedSeq[Part]]]

    def find(id: UUID): Future[\/[RepositoryError, Part]]
    def find(project: Project, position: Int): Future[\/[RepositoryError, Part]]
    def insert(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Part]]
    def update(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Part]]
    def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Part]]
    def delete(project: Project)(implicit conn: Connection):Future[\/[RepositoryError, Part]]

    def reorder(project: Project, parts: IndexedSeq[Part])(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Part]]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Part] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Part]] _
  }
}
