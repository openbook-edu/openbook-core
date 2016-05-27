package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.Account
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.stripe.net.RequestOptions
import org.joda.time.DateTime
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.\/

trait PaymentService extends Service[ErrorUnion#Fail] {
  val db: DB
  val requestOptions: RequestOptions

  def getAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]]
  def createAccount(userId: UUID, status: String, activeUntil: Option[DateTime] = None): Future[\/[ErrorUnion#Fail, Account]]
  def updateAccount(id: UUID, version: Long, status: String, activeUntil: Option[DateTime], customer: Option[JsValue]): Future[\/[ErrorUnion#Fail, Account]]

  def listPlans: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]]
  def getPlan(planId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def createCustomer(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def subscribe(userId: UUID, customerId: String, planId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def hasAccess(userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]
}
