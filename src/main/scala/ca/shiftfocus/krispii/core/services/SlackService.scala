package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.DB
import play.api.Configuration
import play.api.i18n.Lang
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scalaz.\/

trait SlackService extends Service[ErrorUnion#Fail] {
  val db: DB
  val wsClient: WSClient
  val configuration: Configuration

  /**
   * Send a message to a slack channel
   *
   * @param text
   * @param url (Optional)
   * @param channel (Optional)
   * @param username (Optional)
   * @param icon (Optional)
   * @return
   */
  def sendMessage(
    text: String,
    scope: String = "default",
    url: Option[String] = None,
    channel: Option[String] = None,
    username: Option[String] = None,
    icon: Option[String] = None
  ): Future[\/[ErrorUnion#Fail, Unit]]
}
