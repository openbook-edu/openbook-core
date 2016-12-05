package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.PaymentLog

import scala.concurrent.Future
import scalaz.\/

trait PaymentLogRepository extends Repository {
  def list(): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]]
  def list(userId: UUID): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]]
  def insert(paymentLog: PaymentLog): Future[\/[RepositoryError.Fail, PaymentLog]]
}
