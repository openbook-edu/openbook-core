package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.stripe.StripePlan
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait StripePlanRepository extends Repository {
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[StripePlan]]]
  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]]
  def find(stripeId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]]
  def create(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]]
  def update(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]]
  def delete(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]]
}
