package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib.FutureMonad
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.{EitherT, \/-, \/}

trait Service[F] extends FutureMonad[F]  {
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

  def serialized[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[R]): Future[IndexedSeq[R]] = {
    collection.foldLeft(Future(IndexedSeq.empty[R])) { (fAccumulated, nextItem) =>
      for {
        accumulated <- fAccumulated
        nextResult <- fn(nextItem)
      }
      yield accumulated :+ nextResult
    }
  }

  def serializedT[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[\/[F, R]]): Future[\/[F, IndexedSeq[R]]] = {
    val empty: Future[\/[F, IndexedSeq[R]]] = Future.successful(\/-(IndexedSeq.empty[R]))
    collection.foldLeft(empty) { (fAccumulated, nextItem) =>
      val iteration = for {
        accumulated <- lift(fAccumulated)
        nextResult <- lift(fn(nextItem))
      }
      yield accumulated :+ nextResult
      iteration.run
    }
  }
}
