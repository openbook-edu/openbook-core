package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.fail.Fail
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait ComponentRepositoryComponent extends FutureMonad {
  self: PartRepositoryComponent =>

  val componentRepository: ComponentRepository

  trait ComponentRepository {
    def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]]
    def list(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]]
    def list(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]]
    def list(project: Project, user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]]

    def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Component]]

    def insert(component: Component)(implicit conn: Connection): Future[\/[Fail, Component]]
    def update(component: Component)(implicit conn: Connection): Future[\/[Fail, Component]]
    def delete(component: Component)(implicit conn: Connection): Future[\/[Fail, Component]]

    def addToPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromPart(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]]
  }
}
