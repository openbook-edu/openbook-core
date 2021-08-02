package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.datasource.DB
import ca.shiftfocus.lib.concurrent.Lifting
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future

trait Service[F] extends Lifting[F] {

  /**
   * Database connection (pool). Services will take connections from
   * this pool when making repository calls.
   */
  val db: DB

  /**
   * Takes a function that returns a future, and runs it inside a database
   * transaction.
   */
  def transactional[A](f: Connection => Future[A]): Future[A] = {
    db.pool.inTransaction { conn =>
      conn.sendQuery("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ").flatMap { _ =>
        f(conn)
      }
    }.recover {
      case exception => throw exception
    }
  }

}
