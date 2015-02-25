package ca.shiftfocus.krispii.core.lib

import ca.shiftfocus.krispii.core.fail.Fail
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._

trait FutureMonad {
  def lift[A] = EitherT.eitherT[Future, Fail, A] _

  def liftSeq[A](interList: IndexedSeq[Future[\/[Fail, A]]]): EitherT[Future, Fail, IndexedSeq[A]] = {
    liftSeq(Future.sequence(interList))
  }

  def liftSeq[A](fIntermediate: Future[IndexedSeq[\/[Fail, A]]]): EitherT[Future, Fail, IndexedSeq[A]] = {
    val result = fIntermediate.map { intermediate =>
      if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
      else \/-(intermediate.map(_.toOption.get))
    }
    lift(result)
  }

  def predicate(condition: Boolean)(fail: Fail): EitherT[Future, Fail, Unit] = {
    val result = Future.successful {
      if (condition) \/-(())
      else -\/(fail)
    }
    lift(result)
  }

  def predicate(fCondition: Future[Boolean])(fail: Fail): EitherT[Future, Fail, Unit] = {
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