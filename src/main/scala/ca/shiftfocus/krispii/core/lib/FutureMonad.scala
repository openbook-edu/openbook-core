package ca.shiftfocus.krispii.core.lib

import ca.shiftfocus.krispii.core.fail.Fail
import scala.concurrent.Future
import scalaz.{EitherT, Monad}

trait FutureMonad {
  def lift[A] = EitherT.eitherT[Future, Fail, A] _

  implicit val futureMonad = new Monad[Future] {
    override def point[A](a: ⇒ A): Future[A] = Future(a)
    override def bind[A, B](fa: Future[A])(f: A ⇒ Future[B]): Future[B] = fa.flatMap(f)
  }
}