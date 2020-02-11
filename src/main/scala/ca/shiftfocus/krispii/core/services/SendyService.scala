package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.DB
import play.api.Configuration
import play.api.i18n.{Lang}
import play.api.libs.ws.WSClient
import scala.concurrent.Future
import scalaz.\/

trait SendyService extends Service[ErrorUnion#Fail] {
  val db: DB
  val wsClient: WSClient
  val configuration: Configuration

  def subscribe(user: User, account: Account, lang: Lang): Future[\/[ErrorUnion#Fail, Unit]]
}
