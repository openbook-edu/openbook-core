package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib._
import com.shiftfocus.krispii.common.models._
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
