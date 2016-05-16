package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.repositories.CustomerRepository
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.stripe.net.RequestOptions
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.\/

trait PaymentService extends Service[ErrorUnion#Fail] {
  val db: DB
  val requestOptions: RequestOptions

  def listPlans: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]]
  def getPlan(planId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def listUserSubscriptions(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]]
  def getCustomer(userId: UUID): Future[\/[ErrorUnion#Fail, JsValue]]
  def createCustomer(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]]
  def subscribe(userId: UUID, customerId: String, planId: String): Future[\/[ErrorUnion#Fail, JsValue]]
}
