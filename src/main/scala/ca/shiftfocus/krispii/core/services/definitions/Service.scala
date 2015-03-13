package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.lib.concurrent.{Serialized, Lifting, FutureMonad}
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.{EitherT, \/-, \/}

trait Service[F] extends Lifting[F] with Serialized with FutureMonad  {
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
