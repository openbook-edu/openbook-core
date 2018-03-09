package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.DB
import play.api.i18n.Lang
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Logger }
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scalaz.{ \/, \/- }

class SendyServiceDefault(
    val db: DB,
    val wsClient: WSClient,
    val configuration: Configuration
) extends SendyService {

  def subscribe(user: User, account: Account, lang: Lang): Future[\/[ErrorUnion#Fail, Unit]] = {
    val maybeSendyUrl = configuration.get[Option[String]]("sendy.url")
    val maybeSendyListId = {
      if (account.status == AccountStatus.limited) {
        val freeListId = configuration.get[Option[String]](s"sendy.free.${lang.code}.list.id")
        // If there is no free user list, use default one
        freeListId match {
          case Some(list) => Some(list)
          case _ => configuration.get[Option[String]](s"sendy.${lang.code}.list.id")
        }
      }
      else configuration.get[Option[String]](s"sendy.${lang.code}.list.id")
    }

    (maybeSendyUrl, maybeSendyListId) match {
      case (Some(sendyUrl), Some(sendyListId)) => {
        val postBody = Map[String, Seq[String]](
          "name" -> Seq((user.givenname + " " + user.surname)),
          "email" -> Seq(user.email),
          "list" -> Seq(sendyListId),
          "boolean" -> Seq("true")
        )

        wsClient.url(sendyUrl + "/subscribe").withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded").post(postBody).map { response =>
          val result = response.body
          if (result != "1") {
            Logger.error(s"[SENDY ERROR] For ${user.email}: " + result)
          }
        }.recover {
          case e => Logger.error(s"[SENDY ERROR] For ${user.email}: " + e.toString)
        }
      }
      case (None, Some(sendyListId)) => Logger.error(s"[SENDY ERROR] For ${user.email}: Missing configuration: sendy.url")
      case (Some(sendyUrl), None) => Logger.error(s"[SENDY ERROR] For ${user.email}: Missing configuration: " + s"sendy.${lang.code}.list.id")
      case _ => Logger.error(s"[SENDY ERROR] For ${user.email}: Missing configuration: sendy.url and " + s"sendy.${lang.code}.list.id")
    }

    Future successful \/-((): Unit)
  }
}
