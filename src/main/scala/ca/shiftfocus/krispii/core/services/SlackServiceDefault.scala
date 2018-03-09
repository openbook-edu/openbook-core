package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.services.datasource.DB
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Logger }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ \/, \/- }

class SlackServiceDefault(
    val db: DB,
    val wsClient: WSClient,
    val configuration: Configuration
) extends SlackService {

  def sendMessage(
    text: String,
    scope: String = "default",
    url: Option[String] = None,
    channel: Option[String] = None,
    username: Option[String] = None,
    icon: Option[String] = None
  ): Future[\/[ErrorUnion#Fail, Unit]] = {
    val urlData = url match {
      case Some(url) => url
      case _ => configuration.get[Option[String]](s"slack.${scope}.url").getOrElse("")
    }
    val channelData = channel match {
      case Some(channel) => url
      case _ => configuration.get[Option[String]](s"slack.${scope}.channel").getOrElse("")
    }
    val usernameData = username match {
      case Some(channel) => url
      case _ => configuration.get[Option[String]](s"slack.${scope}.username").getOrElse("")
    }
    val icon_emojiData = icon match {
      case Some(channel) => url
      case _ => configuration.get[Option[String]](s"slack.${scope}.icon_emoji").getOrElse("")
    }
    val postBody =
      s"""payload={
          |"channel":"${channelData}",
          |"username":"${usernameData}",
          |"text":"${text}",
          |"icon_emoji":"${icon_emojiData}"
          |}""".stripMargin

    try {
      wsClient.url(urlData).withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8").post(postBody).map { response =>
        if (response.body != "ok") {
          Logger.error(s"[SLACK ERROR] failed to send a message: ${text}. With an error: " + response.body)
        }
      }.recover {
        case e => Logger.error(s"[SLACK ERROR] failed to send a message: ${text}. With an error: " + e.toString)
      }
    }
    catch {
      case e: Throwable => {
        Logger.error(s"[SLACK ERROR] failed to send a message: ${text}. With an error: " + e.toString)
      }
    }

    Future successful \/-((): Unit)
  }
}
