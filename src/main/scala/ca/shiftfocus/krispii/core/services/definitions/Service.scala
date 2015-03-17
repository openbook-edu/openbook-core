package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.lib.concurrent.Lifting
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future

trait Service[F] extends Lifting[F] {

  /**
   * Database connection (pool). Services will take connections from
   * this pool when making repository calls.
   */
  val db: Connection

  /**
   * Takes a function that returns a future, and runs it inside a database
   * transaction.
   */
  def transactional[A](f : Connection => Future[A]) = {
    db.inTransaction(f).recover {
      case exception => throw exception
    }
  }

}
