package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import scalaz.{ \/ }

trait ComponentScratchpadRepository extends Repository {
  val userRepository: UserRepository
  val componentRepository: ComponentRepository

  def list(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ComponentScratchpad]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ComponentScratchpad]]]

  def find(user: User, component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ComponentScratchpad]]

  def insert(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ComponentScratchpad]]
  def update(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ComponentScratchpad]]
  def delete(componentScratchpad: ComponentScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ComponentScratchpad]]
}
