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
  def getAccount(customerId: String): Future[\/[ErrorUnion#Fail, Account]]
  def createAccount(userId: UUID, status: String, activeUntil: Option[DateTime] = None): Future[\/[ErrorUnion#Fail, Account]]
  def updateAccount(id: UUID, version: Long, status: String, activeUntil: Option[DateTime], customer: Option[JsValue],
    overdueStartedAt: Option[Option[DateTime]] = None, overdueEndedAt: Option[Option[DateTime]] = None, overduePlanId: Option[Option[String]] = None): Future[\/[ErrorUnion#Fail, Account]]

  def listPlansFromStripe: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]]
  def fetchPlanFromStripe(planId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def createCustomer(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def updateCustomer(userId: UUID, email: String, givenname: String, surname: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def fetchCustomerFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def deleteCustomer(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def subscribe(userId: UUID, customerId: String, planId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def updateSubscribtionPlan(userId: UUID, subscriptionId: String, newPlanId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def updateSubscription(userId: UUID, subscriptionId: String, subscription: JsValue): Future[\/[ErrorUnion#Fail, JsValue]]
  def cancelSubscription(userId: UUID, subscriptionId: String, atPeriodEnd: Boolean): Future[\/[ErrorUnion#Fail, JsValue]]
  def deleteSubscription(userId: UUID, subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def createInvoiceItem(customerId: String, amount: Int, currency: String, description: String = ""): Future[\/[ErrorUnion#Fail, JsValue]]
  def updatePaymentInfo(customerId: String, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def deletePaymentInfo(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def fetchSubscriptionFromStripe(subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def hasAccess(userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]

  def fetchEventFromStripe(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def getEvent(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def createEvent(eventId: String, eventType: String, event: JsValue): Future[\/[ErrorUnion#Fail, JsValue]]
}
