package ca.shiftfocus.krispii.core.lib

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._

trait FutureMonad[E] {

  implicit def eitherRunner[A](eithert: EitherT[Future, E, A]): Future[\/[E, A]] = eithert.run

  def lift[A] = EitherT.eitherT[Future, E, A] _

  def liftSeq[A](interList: IndexedSeq[Future[\/[E, A]]]): EitherT[Future, E, IndexedSeq[A]] = {
    liftSeq(Future.sequence(interList))
  }

  def liftSeq[A](fIntermediate: Future[IndexedSeq[\/[E, A]]]): EitherT[Future, E, IndexedSeq[A]] = {
    val result = fIntermediate.map { intermediate =>
      if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
      else \/-(intermediate.map(_.toOption.get))
    }
    lift(result)
  }

  def predicate(condition: Boolean)(fail: E): EitherT[Future, E, Unit] = {
    val result = Future.successful {
      if (condition) \/-(())
      else -\/(fail)
    }
    lift(result)
  }

  def predicate(fCondition: Future[Boolean])(fail: E): EitherT[Future, E, Unit] = {
    lift {
      fCondition.map { condition =>
        if (condition) \/-(())
        else -\/(fail)
      }
    }
  }

  implicit val futureMonad = new Monad[Future] {
    override def point[A](a: ⇒ A): Future[A] = Future(a)
    override def bind[A, B](fa: Future[A])(f: A ⇒ Future[B]): Future[B] = fa.flatMap(f)
  }
}