package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import error._
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait ComponentRepositoryComponent {
  self: PartRepositoryComponent =>

  val componentRepository: ComponentRepository

  trait ComponentRepository {
    def list(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Component]]]
    def list(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Component]]]
    def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Component]]]
    def list(project: Project, user: User)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Component]]]

    def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError, Component]]

    def insert(component: Component)(implicit conn: Connection): Future[\/[RepositoryError, Component]]
    def update(component: Component)(implicit conn: Connection): Future[\/[RepositoryError, Component]]
    def delete(component: Component)(implicit conn: Connection): Future[\/[RepositoryError, Component]]

    def addToPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Component]]
    def removeFromPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Component]]
    def removeFromPart(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, Component]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Component] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Component]] _
  }
}
