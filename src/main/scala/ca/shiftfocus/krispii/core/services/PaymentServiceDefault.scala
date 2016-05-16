package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ ErrorUnion, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.repositories.{ CustomerRepository, SubscriptionRepository, UserRepository }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import com.stripe.model.{ Customer, Plan, Subscription }
import com.stripe.net.{ APIResource, RequestOptions }
import play.api.libs.json.{ JsValue, Json }

import collection.JavaConversions._
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val requestOptions: RequestOptions,
    val userRepository: UserRepository,
    val customerRepository: CustomerRepository,
    val subscriptionRepository: SubscriptionRepository
) extends PaymentService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  def listPlans: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]] = Future {
    try {
      val params = new java.util.HashMap[String, Object]()

      val planList: IndexedSeq[JsValue] = Plan.list(params, requestOptions).getData.map(plan => {
        Json.parse(APIResource.GSON.toJson(plan))
      }).toIndexedSeq

      \/-(planList)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  def getPlan(planId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val plan: JsValue = Json.parse(APIResource.GSON.toJson(Plan.retrieve(planId, requestOptions)))

      \/-(plan)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  def listUserSubscriptions(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]] = {
    subscriptionRepository.listSubscriptions(userId)
  }

  def getCustomer(userId: UUID): Future[\/[ErrorUnion#Fail, JsValue]] = {
    customerRepository.getCustomer(userId)
  }

  def createCustomer(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      user <- lift(userRepository.find(userId))
      customer <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("source", tokenId)
          params.put("description", user.givenname + " " + user.surname)
          params.put("email", user.email)

          val customer: JsValue = Json.parse(APIResource.GSON.toJson(Customer.create(params, requestOptions)))

          \/-(customer)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(customerRepository.createCustomer(userId, customer))
    } yield customer
  }

  def subscribe(userId: UUID, customerId: String, planId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      subscription <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("customer", customerId)
          params.put("plan", planId)

          val subscription: JsValue = Json.parse(APIResource.GSON.toJson(Subscription.create(params, requestOptions)))

          \/-(subscription)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(subscriptionRepository.createSubscription(userId, subscription))
    } yield subscription
  }
}

