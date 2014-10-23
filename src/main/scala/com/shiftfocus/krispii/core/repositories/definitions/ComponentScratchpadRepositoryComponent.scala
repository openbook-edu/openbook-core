package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib._
import com.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait ComponentScratchpadRepositoryComponent {

  val componentScratchpadRepository: ComponentScratchpadRepository

  trait ComponentScratchpadRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list(component: Component)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]]
    def list(user: User)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]]
    def list(user: User, component: Component)(implicit conn: Connection): Future[IndexedSeq[ComponentScratchpad]]
    def find(user: User, component: Component)(implicit conn: Connection): Future[Option[ComponentScratchpad]]
    def find(user: User, component: Component, revision: Long)(implicit conn: Connection): Future[Option[ComponentScratchpad]]
    def insert(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[ComponentScratchpad]
    def update(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[ComponentScratchpad]
    def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[Boolean]
  }
}
