package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.\/

trait CustomerRepository extends Repository {
  def getCustomer(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def createCustomer(userId: UUID, customer: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
}
