package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.PaymentLog
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait PaymentLogRepository extends Repository {
  def list()(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]]
  def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]]
  def move(oldUserId: UUID, newUserId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]]
  def insert(paymentLog: PaymentLog)(implicit conn: Connection): Future[\/[RepositoryError.Fail, PaymentLog]]
}
