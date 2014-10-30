package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait ComponentRepositoryComponent {
  val componentRepository: ComponentRepository

  trait ComponentRepository {
    def list(implicit conn: Connection): Future[IndexedSeq[Component]]
    def list(part: Part)(implicit conn: Connection): Future[IndexedSeq[Component]]
    def list(project: Project, user: User)(implicit conn: Connection): Future[IndexedSeq[Component]]

    def find(id: UUID)(implicit conn: Connection): Future[Option[Component]]

    def insert(component: Component)(implicit conn: Connection): Future[Component]
    def update(component: Component)(implicit conn: Connection): Future[Component]
    //def delete(component: Component)(implicit conn: Connection): Future[Boolean]

    def addToPart(component: Component, part: Part)(implicit conn: Connection): Future[Boolean]
    def removeFromPart(component: Component, part: Part)(implicit conn: Connection): Future[Boolean]
    def removeFromPart(part: Part)(implicit conn: Connection): Future[Boolean]
  }
}
