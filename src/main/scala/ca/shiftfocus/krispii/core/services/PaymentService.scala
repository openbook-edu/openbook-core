package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError, ServiceError}
import ca.shiftfocus.krispii.core.models.stripe.{CreditCard, StripePlan, StripeSubscription}
import ca.shiftfocus.krispii.core.models.user.User
import ca.shiftfocus.krispii.core.models.{Account, PaymentLog}
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.stripe.model.{Card, Customer, Invoice, InvoiceItem, Subscription}
import com.stripe.net.RequestOptions
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import scalaz.\/

import scala.collection.immutable.TreeMap
import scala.concurrent.Future

trait PaymentService extends Service[ErrorUnion#Fail] {
  val db: DB
  val requestOptions: RequestOptions

  def getAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]]
  def getAccount(customerId: String): Future[\/[ErrorUnion#Fail, Account]]
  def createAccount(userId: UUID, status: String, activeUntil: Option[DateTime] = None): Future[\/[ErrorUnion#Fail, Account]]
  def updateAccount(id: UUID, version: Long, status: String, trialStartedAt: Option[Option[DateTime]], activeUntil: Option[DateTime],
    overdueStartedAt: Option[Option[DateTime]] = None, overdueEndedAt: Option[Option[DateTime]] = None,
    overduePlanId: Option[Option[String]] = None): Future[\/[ErrorUnion#Fail, Account]]
  def deleteAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]]

  // TODO: The JsValues from stripe should probably not be public
  def listPlansFromStripe: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]]
  def fetchPlanFromStripe(planId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def listPlansFromDb: Future[\/[ErrorUnion#Fail, IndexedSeq[StripePlan]]]
  def findPlanInDb(id: UUID): Future[\/[ErrorUnion#Fail, StripePlan]]
  def findPlanInDb(planId: String): Future[\/[ErrorUnion#Fail, StripePlan]]
  def savePlanInDb(planId: String, title: String): Future[\/[ErrorUnion#Fail, StripePlan]]
  def updatePlanInDb(id: UUID, version: Long, title: String): Future[\/[ErrorUnion#Fail, StripePlan]]
  def deletePlanFromDb(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, StripePlan]]

  // TODO: It would be better to move the machinery that deals with Customer from API into core
  def fetchCustomerFromStripe(customerId: String): ServiceError.ExternalService \/ Customer
  def getCreditCard(account: Account): \/[ErrorUnion#Fail, CreditCard]
  def getCreditCard(customerId: String): Future[RepositoryError.Fail \/ CreditCard]
  def getCreditCard(accountId: UUID): Future[RepositoryError.Fail \/ CreditCard]
  def createCreditCard(account: Account, tokenId: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  def createCreditCard(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  def updateCreditCardFromStripe(card: CreditCard, userId: UUID, email: String, givenname: String, surname: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  def updateCreditCardFromStripe(userId: UUID, email: String, givenname: String, surname: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  def updateCreditCardFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  // the JsValue from Stripe should not be public
  // def fetchCustomerFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def deleteCustomerAndCreditCard(customerId: String): Future[\/[ErrorUnion#Fail, CreditCard]]

  def subscribe(userId: UUID, customerId: String, planId: String): Future[\/[ErrorUnion#Fail, StripeSubscription]]
  // better to hide Stripe Subscription class from API, but leave for the moment
  def fetchSubscriptionFromStripe(subscriptionId: String): ErrorUnion#Fail \/ Subscription
  def fetchSubscriptionFromDb(subscriptionId: String): Future[ErrorUnion#Fail \/ StripeSubscription]
  def updateSubscriptionPlan(subscriptionId: String, newPlanId: String): Future[\/[ErrorUnion#Fail, StripeSubscription]]
  def updateSubscription(subscriptionId: String, currentPeriodEnd: Long, cancelAtPeriodEnd: Boolean): Future[\/[ErrorUnion#Fail, StripeSubscription]]
  def cancelSubscription(subscriptionId: String, atPeriodEnd: Boolean): Future[\/[ErrorUnion#Fail, StripeSubscription]]
  def deleteSubscription(subscriptionId: String): Future[\/[ErrorUnion#Fail, StripeSubscription]]
  def fetchUpcomingInvoiceFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, Invoice]]
  def listInvoiceItemsFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, List[InvoiceItem]]]
  // TODO: the JsValue, and the stripe Card objects, should not be public
  def createInvoiceItem(customerId: String, amount: Int, currency: String, description: String = "", metadata: TreeMap[String, Object] = TreeMap.empty): Future[\/[ErrorUnion#Fail, JsValue]]
  def fetchPaymentInfoFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, Card]]
  def updatePaymentInfo(user: User, account: Account, customerId: String, tokenId: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  def deletePaymentInfo(customerId: String): Future[\/[ErrorUnion#Fail, CreditCard]]
  // def fetchSubscriptionFromStripe(subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]]

  def hasAccess(userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]

  // TODO: replace the JsValue in stripe events by a case class
  def fetchEventFromStripe(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def getEvent(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def createEvent(eventId: String, eventType: String, event: JsValue): Future[\/[ErrorUnion#Fail, JsValue]]

  def listLog(): Future[\/[ErrorUnion#Fail, IndexedSeq[PaymentLog]]]
  def listLog(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[PaymentLog]]]
  def createLog(logType: String, description: String, data: String, userId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, PaymentLog]]
}
