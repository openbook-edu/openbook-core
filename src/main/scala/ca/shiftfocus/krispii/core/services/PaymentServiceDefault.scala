package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.{ Account, AccountStatus }
import ca.shiftfocus.krispii.core.repositories.{ AccountRepository, StripeRepository, UserRepository }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import com.stripe.model._
import com.stripe.net.{ APIResource, RequestOptions }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsValue, Json }

import collection.JavaConversions._
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val requestOptions: RequestOptions,
    val userRepository: UserRepository,
    val accountRepository: AccountRepository,
    val stripeRepository: StripeRepository
) extends PaymentService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  def getAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      account <- lift(accountRepository.getByUserId(userId))
      subscriptions <- lift(stripeRepository.listSubscriptions(userId))
    } yield account.copy(subscriptions = subscriptions)
  }

  def getAccount(customerId: String): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      account <- lift(accountRepository.getByCustomerId(customerId))
      subscriptions <- lift(stripeRepository.listSubscriptions(account.userId))
    } yield account.copy(subscriptions = subscriptions)
  }

  def createAccount(userId: UUID, status: String, activeUntil: Option[DateTime] = None): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      user <- lift(userRepository.find(userId))
      account <- lift(accountRepository.insert(
        Account(
          userId = userId,
          status = status,
          activeUntil = activeUntil
        )
      ))
    } yield account
  }

  def updateAccount(
    id: UUID,
    version: Long,
    status: String,
    activeUntil: Option[DateTime],
    customer: Option[JsValue]
  ): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      existingAccount <- lift(accountRepository.get(id))
      subscriptions <- lift(stripeRepository.listSubscriptions(existingAccount.userId))
      updatedAccount <- lift(accountRepository.update(existingAccount.copy(
        status = status,
        activeUntil = activeUntil,
        customer = customer
      )))
    } yield updatedAccount.copy(subscriptions = subscriptions)
  }

  def listPlansFromStripe: Future[\/[ErrorUnion#Fail, IndexedSeq[JsValue]]] = Future {
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

  def fetchPlanFromStripe(planId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val plan: JsValue = Json.parse(APIResource.GSON.toJson(Plan.retrieve(planId, requestOptions)))

      \/-(plan)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  def createCustomer(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      user <- lift(userRepository.find(userId))
      account <- lift(accountRepository.getByUserId(userId))
      customer <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("source", tokenId)
          params.put("description", user.givenname + " " + user.surname + " - " + user.id.toString)
          params.put("email", user.email)

          val customer = Customer.create(params, requestOptions)
          // Get default payment source
          val defaultSource = customer.getSources().retrieve(customer.getDefaultSource, requestOptions)
          val result: JsValue = defaultSource.getObject match {
            // If we have a card, we get additional information about it
            case "card" => {
              val defaultCard = defaultSource.asInstanceOf[Card]
              val last4 = defaultCard.getLast4()
              val brand = defaultCard.getBrand()
              val expYear = defaultCard.getExpYear()
              val expMonth = defaultCard.getExpMonth()
              val customerJObject = Json.parse(APIResource.GSON.toJson(customer)).as[JsObject]
              val defaultCardJObject = Json.parse(APIResource.GSON.toJson(defaultCard)).as[JsObject]

              // Add additional card info to customer object
              customerJObject ++ Json.obj(
                "sources" -> Json.obj(
                  "data" -> Json.arr(
                    defaultCardJObject ++ Json.obj(
                      "last4" -> last4,
                      "brand" -> brand,
                      "exp_year" -> expYear.toString,
                      "exp_month" -> expMonth.toString
                    )
                  )
                )
              )
            }
            case _ => Json.parse(APIResource.GSON.toJson(customer))
          }

          \/-(result)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
    } yield customer
  }

  /**
   * Get customer from Stripe
   *
   * @param customerId
   * @return
   */
  def fetchCustomerFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val customer: JsValue = Json.parse(APIResource.GSON.toJson(Customer.retrieve(customerId, requestOptions)))

      \/-(customer)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  /**
   * Delete customer from our DB and Stripe
   *
   * @param customerId
   * @return
   */
  def deleteCustomer(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      account <- lift(accountRepository.getByCustomerId(customerId))
      _ <- lift(
        try {
          val customerObj = Customer.retrieve(customerId, requestOptions)
          customerObj.delete(requestOptions)
          Future successful \/-(account.customer.getOrElse(Json.parse("{}")))
        }
        catch {
          case error => Future successful -\/(ServiceError.ExternalService(error.toString))
        }
      )
      updatedAccount <- lift(accountRepository.update(account.copy(
        customer = None
      )))
    } yield account.customer.getOrElse(Json.parse("{}"))
  }

  /**
   * Subscribe customer to a specific plan
   *
   * @param userId
   * @param customerId
   * @param planId
   * @return
   */
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
      _ <- lift(stripeRepository.createSubscription(userId, subscription))
    } yield subscription
  }

  /**
   * Switch subscription to a new plan
   *
   * @param userId
   * @param  subscriptionId
   * @param newPlanId
   * @return
   */
  def updateSubscribtionPlan(userId: UUID, subscriptionId: String, newPlanId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      updatedSubsctiption <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("plan", newPlanId)

          val subscription: Subscription = Subscription.retrieve(subscriptionId, requestOptions)
          val updatedSubsctiption = subscription.update(params, requestOptions)

          \/-(updatedSubsctiption)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(stripeRepository.updateSubscription(userId, updatedSubsctiption.getId, Json.parse(APIResource.GSON.toJson(updatedSubsctiption))))
    } yield Json.parse(APIResource.GSON.toJson(updatedSubsctiption))
  }

  def cancelSubscription(userId: UUID, subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      canceledSubsctiption <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("at_period_end", "true")

          val subscription: Subscription = Subscription.retrieve(subscriptionId, requestOptions)
          val canceledSubsctiption = subscription.cancel(params, requestOptions)

          \/-(canceledSubsctiption)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(stripeRepository.updateSubscription(userId, canceledSubsctiption.getId, Json.parse(APIResource.GSON.toJson(canceledSubsctiption))))
    } yield Json.parse(APIResource.GSON.toJson(canceledSubsctiption))
  }

  /**
   * Replace customer card with a new one
   *
   * @param customerId
   * @param tokenId
   * @return
   */
  def updatePaymentInfo(customerId: String, tokenId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      updatedCustomer <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("source", tokenId)

          val customer: Customer = Customer.retrieve(customerId, requestOptions)
          val updatedCustomer = customer.update(params, requestOptions)

          // Get default payment source
          val defaultSource = updatedCustomer.getSources().retrieve(updatedCustomer.getDefaultSource, requestOptions)
          val result: JsValue = defaultSource.getObject match {
            // If we have a card, we get additional information about it
            case "card" => {
              val defaultCard = defaultSource.asInstanceOf[Card]
              val last4 = defaultCard.getLast4()
              val brand = defaultCard.getBrand()
              val expYear = defaultCard.getExpYear()
              val expMonth = defaultCard.getExpMonth()
              val customerJObject = Json.parse(APIResource.GSON.toJson(updatedCustomer)).as[JsObject]
              val defaultCardJObject = Json.parse(APIResource.GSON.toJson(defaultCard)).as[JsObject]

              // Add additional card info to customer object
              customerJObject ++ Json.obj(
                "sources" -> Json.obj(
                  "data" -> Json.arr(
                    defaultCardJObject ++ Json.obj(
                      "last4" -> last4,
                      "brand" -> brand,
                      "exp_year" -> expYear.toString,
                      "exp_month" -> expMonth.toString
                    )
                  )
                )
              )
            }
            case _ => Json.parse(APIResource.GSON.toJson(updatedCustomer))
          }

          \/-(result)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
    } yield updatedCustomer
  }

  def deletePaymentInfo(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      updatedCustomer <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()

          val customer: Customer = Customer.retrieve(customerId, requestOptions)
          // Get default payment source and delete it
          val defaultSource = customer.getSources().retrieve(customer.getDefaultSource, requestOptions)
          defaultSource.delete(requestOptions)

          val result: JsValue = {
            val customerJObject = Json.parse(APIResource.GSON.toJson(customer)).as[JsObject]

            // Clean card info
            customerJObject.as[JsObject] - "sources"
          }

          \/-(result)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
    } yield updatedCustomer
  }

  /**
   * Get subscription from Stripe
   *
   * @param subscriptionId
   * @return
   */
  def fetchSubscriptionFromStripe(subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val subscription: JsValue = Json.parse(APIResource.GSON.toJson(Subscription.retrieve(subscriptionId, requestOptions)))

      \/-(subscription)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  /**
   * Update subscription in our DB
   *
   * @param userId
   * @param subscription
   * @return
   */
  def updateSubscription(userId: UUID, subscription: JsValue): Future[\/[ErrorUnion#Fail, JsValue]] = {
    val subscriptionId = (subscription \ "id").asOpt[String].getOrElse("")
    for {
      updatedSubscription <- lift(stripeRepository.updateSubscription(userId, subscriptionId, subscription))
    } yield updatedSubscription
  }

  /**
   * Check if user has access to the application
   *
   * @param userId
   * @return
   */
  def hasAccess(userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      hasAccess <- lift(accountRepository.getByUserId(userId).map {
        case \/-(account) => \/-(
          if (account.status == AccountStatus.free || // FREE
            account.activeUntil.isDefined && account.activeUntil.get.isAfterNow) { // Active until date is greater then now
            true
          }
          else {
            false
          }
        )
        case -\/(error: RepositoryError.NoResults) => \/-(false)
        case -\/(error) => -\/(error)
      })
    } yield hasAccess
  }

  /**
   * Get event from Stripe
   *
   * @param eventId
   * @return
   */
  def fetchEventFromStripe(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val event: JsValue = Json.parse(APIResource.GSON.toJson(Event.retrieve(eventId, requestOptions)))

      \/-(event)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  /**
   * Get event from our database
   *
   * @param eventId
   * @return
   */
  def getEvent(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      event <- lift(stripeRepository.getEvent(eventId))
    } yield event
  }

  def createEvent(eventId: String, eventType: String, event: JsValue): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      event <- lift(stripeRepository.createEvent(eventId, eventType, event))
    } yield event
  }
}

