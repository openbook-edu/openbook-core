package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait ComponentScratchpadRepositoryComponent extends FutureMonad {
  self: UserRepositoryComponent with
        ComponentRepositoryComponent =>

  val componentScratchpadRepository: ComponentScratchpadRepository

  trait ComponentScratchpadRepository {
    def list(component: Component)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]]
    def list(user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]]

    def find(user: User, component: Component)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]]

    def insert(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]]
    def update(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]]
    def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[Fail, ComponentScratchpad]]
  }
}
