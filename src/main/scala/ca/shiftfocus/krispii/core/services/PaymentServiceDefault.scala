package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.stripe.StripePlan
import ca.shiftfocus.krispii.core.models.{ Account, AccountStatus, PaymentLog, TaggableEntities }
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import com.stripe.exception.InvalidRequestException
import com.stripe.model._
import com.stripe.net.{ APIResource, RequestOptions }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsValue, Json }

import collection.JavaConversions._
import scala.collection.immutable.TreeMap
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val requestOptions: RequestOptions,
    val userRepository: UserRepository,
    val accountRepository: AccountRepository,
    val stripeRepository: StripeRepository,
    val paymentLogRepository: PaymentLogRepository,
    val tagRepository: TagRepository,
    val stripePlanRepository: StripePlanRepository
) extends PaymentService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /**
   * Get user account with subscriptions from krispii db by userId
   *
   * @param userId
   * @return
   */
  def getAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      account <- lift(accountRepository.getByUserId(userId))
      subscriptions <- lift(stripeRepository.listSubscriptions(userId))
    } yield account.copy(subscriptions = subscriptions)
  }

  /**
   * Get user account with subscriptions from krispii db by stripe customer id
   *
   * @param customerId
   * @return
   */
  def getAccount(customerId: String): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      account <- lift(accountRepository.getByCustomerId(customerId))
      subscriptions <- lift(stripeRepository.listSubscriptions(account.userId))
    } yield account.copy(subscriptions = subscriptions)
  }

  /**
   * Create krispii user account in database
   *
   * @param userId
   * @param status
   * @param activeUntil
   * @return
   */
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
      _ <- lift(tagUntagUserBasedOnStatus(userId, status))
    } yield account
  }

  /**
   *  Update krispii user account information in database
   *
   * @param id
   * @param version
   * @param status
   * @param activeUntil
   * @param customer
   * @param overdueStartedAt The Date when overdue period has started
   * @param overdueEndedAt The Date when overdue period has ended
   * @return  Account with subscriptions
   */
  def updateAccount(
    id: UUID,
    version: Long,
    status: String,
    trialStartedAt: Option[Option[DateTime]],
    activeUntil: Option[DateTime],
    customer: Option[JsValue],
    overdueStartedAt: Option[Option[DateTime]] = None,
    overdueEndedAt: Option[Option[DateTime]] = None,
    overduePlanId: Option[Option[String]] = None
  ): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      existingAccount <- lift(accountRepository.get(id))
      subscriptions <- lift(stripeRepository.listSubscriptions(existingAccount.userId))
      updatedAccount <- lift(accountRepository.update(existingAccount.copy(
        status = status,
        trialStartedAt = trialStartedAt match {
        case Some(Some(trialStartedAt)) => Some(trialStartedAt)
        case Some(None) => None
        case None => existingAccount.trialStartedAt
      },
        activeUntil = activeUntil,
        customer = customer,
        overdueStartedAt = overdueStartedAt match {
        case Some(Some(overdueAt)) => Some(overdueAt)
        case Some(None) => None
        case None => existingAccount.overdueStartedAt
      },
        overdueEndedAt = overdueEndedAt match {
        case Some(Some(overdueAt)) => Some(overdueAt)
        case Some(None) => None
        case None => existingAccount.overdueEndedAt
      },
        overduePlanId = overduePlanId match {
        case Some(Some(overduePlanId)) => Some(overduePlanId)
        case Some(None) => None
        case None => existingAccount.overduePlanId
      }
      )))
      _ <- lift(tagUntagUserBasedOnStatus(updatedAccount.userId, status, Some(existingAccount.status)))
    } yield updatedAccount.copy(subscriptions = subscriptions)
  }

  def deleteAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]] = {
    accountRepository.delete(userId)
  }

  /**
   * List all available plans from stripe
   *
   * @return
   */
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

  /**
   * Get a plan info from stipe by id
   *
   * @param planId
   * @return
   */
  def fetchPlanFromStripe(planId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val plan: JsValue = Json.parse(APIResource.GSON.toJson(Plan.retrieve(planId, requestOptions)))

      \/-(plan)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  def listPlansFromDb: Future[\/[ErrorUnion#Fail, IndexedSeq[StripePlan]]] = {
    stripePlanRepository.list
  }

  def findPlanInDb(id: UUID): Future[\/[ErrorUnion#Fail, StripePlan]] = {
    stripePlanRepository.find(id)
  }

  def findPlanInDb(planId: String): Future[\/[ErrorUnion#Fail, StripePlan]] = {
    stripePlanRepository.find(planId)
  }

  def savePlanInDb(planId: String, title: String): Future[\/[ErrorUnion#Fail, StripePlan]] = {
    stripePlanRepository.create(StripePlan(
      stripeId = planId,
      title = title
    ))
  }

  def updatePlanInDb(id: UUID, version: Long, title: String): Future[\/[ErrorUnion#Fail, StripePlan]] = {
    for {
      existingStripePlan <- lift(stripePlanRepository.find(id))
      _ <- predicate(existingStripePlan.version == version)(RepositoryError.OfflineLockFail)
      updatedStripePlan <- lift(stripePlanRepository.update(existingStripePlan.copy(
        title = title
      )))
    } yield updatedStripePlan
  }

  def deletePlanFromDb(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, StripePlan]] = {
    for {
      existingStripePlan <- lift(stripePlanRepository.find(id))
      _ <- predicate(existingStripePlan.version == version)(RepositoryError.OfflineLockFail)
      deletedStripePlan <- lift(stripePlanRepository.delete(existingStripePlan))
    } yield deletedStripePlan
  }

  /**
   * Create stripe customer
   *
   * @param userId
   * @param tokenId
   * @return
   */
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

  def updateCustomer(userId: UUID, email: String, givenname: String, surname: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      account <- lift(getAccount(userId))
      customerId = (account.customer.get \ "id").asOpt[String].getOrElse("")
      updatedCustomer <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("description", givenname + " " + surname + " - " + account.userId.toString)
          params.put("email", email)

          val customer: Customer = Customer.retrieve(customerId, requestOptions)
          val updatedCustomer = customer.update(params, requestOptions)
          val defaultSource = Option(updatedCustomer.getDefaultSource)

          // Get default payment source
          val result: JsValue = Option(updatedCustomer.getSources()) match {
            case Some(sources) if (defaultSource.isDefined) => {
              val defaultSource = updatedCustomer.getSources().retrieve(updatedCustomer.getDefaultSource, requestOptions)
              defaultSource.getObject match {
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

  /**
   * Get customer from  Stripe
   *
   * @param customerId
   * @return
   */
  def fetchCustomerFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, JsValue]] = Future {
    try {
      val customer: Customer = Customer.retrieve(customerId, requestOptions)
      val defaultSource = Option(customer.getDefaultSource)
      // Get default payment source
      val result: JsValue = Option(customer.getSources()) match {
        case Some(sources) if (defaultSource.isDefined) => {
          val defaultSource = customer.getSources().retrieve(customer.getDefaultSource, requestOptions)
          defaultSource.getObject match {
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
        }
        case _ => Json.parse(APIResource.GSON.toJson(customer))
      }

      \/-(result)
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
   * Subscribe customer to a specific plan in stripe and create subscription info in Krispii db
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
   * Switch subscription to a new plan in stripe and update subscription info in Krispii db.
   * Or resubscribe to the same plan.
   * If we switch plans and if plans intervals match we manually create an invoice and
   * payment is automatically attempted between 1 and 2 hours after the invoice is created
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
          val currentPlan: Plan = subscription.getPlan
          val updatedSubsctiption = subscription.update(params, requestOptions)
          val newPlan: Plan = updatedSubsctiption.getPlan

          // If we switch plans and
          // If plans intervals match then we should manually create an invoice and force payment
          if (currentPlan.getId != newPlan.getId && currentPlan.getInterval == newPlan.getInterval) {
            val customerId = subscription.getCustomer
            val invoiceParams = new java.util.HashMap[String, Object]()
            invoiceParams.put("customer", customerId)
            // Note that manually creating an invoice does not lead to a synchronous attempt to pay the invoice.
            // Payment is automatically attempted between 1 and 2 hours after the invoice is created.
            Invoice.create(invoiceParams, requestOptions)
          }

          \/-(updatedSubsctiption)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(stripeRepository.updateSubscription(userId, updatedSubsctiption.getId, Json.parse(APIResource.GSON.toJson(updatedSubsctiption))))
    } yield Json.parse(APIResource.GSON.toJson(updatedSubsctiption))
  }

  /**
   * Update subscription in Krispii DB
   *
   * @param userId
   * @param subscriptionId
   * @return
   */
  def updateSubscription(userId: UUID, subscriptionId: String, subscription: JsValue): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      updateddSubsctiption <- lift(stripeRepository.updateSubscription(userId, subscriptionId, subscription))
    } yield updateddSubsctiption
  }

  /**
   * Cancel stripe subscription at_period_end or immediately.
   *
   * @param userId
   * @param subscriptionId
   * @param atPeriodEnd If true, then set cancel at_period_end of the subscription to true in stripe and update subscription info in Krispii db,
   *                    If false, then cancel the subscription immediately in stripe and delete it from Krispii db.
   * @return
   */
  def cancelSubscription(userId: UUID, subscriptionId: String, atPeriodEnd: Boolean): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      canceledSubsctiption <- lift(
        Future successful (try {
          val subscription: Subscription = Subscription.retrieve(subscriptionId, requestOptions)
          val canceledSubsctiption = {
            if (atPeriodEnd) {
              var params = new java.util.HashMap[String, Object]()
              params.put("at_period_end", "true")
              subscription.cancel(params, requestOptions)
            }
            else {
              subscription.cancel(null, requestOptions)
            }
          }

          \/-(canceledSubsctiption)
        }
        catch {
          case e => {
            -\/(ServiceError.ExternalService(e.toString))
          }
        })
      )
      _ <- (
        if (atPeriodEnd) {
          lift(stripeRepository.updateSubscription(userId, canceledSubsctiption.getId, Json.parse(APIResource.GSON.toJson(canceledSubsctiption))))
        }
        else {
          lift(stripeRepository.deleteSubscription(userId, canceledSubsctiption.getId))
        }
      )
    } yield Json.parse(APIResource.GSON.toJson(canceledSubsctiption))
  }

  /**
   * Delete subscription from Krispii DB
   *
   * @param userId
   * @param subscriptionId
   * @return
   */
  def deleteSubscription(userId: UUID, subscriptionId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      deletedSubsctiption <- lift(stripeRepository.deleteSubscription(userId, subscriptionId))
    } yield deletedSubsctiption
  }

  def fetchUpcomingInvoiceFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, Invoice]] = {
    Future successful (try {
      val params = new java.util.HashMap[String, Object]()
      params.put("customer", customerId)

      val invoice: Invoice = Invoice.upcoming(params, requestOptions)

      \/-(invoice)
    }
    catch {
      case e: InvalidRequestException if (e.toString.contains("No upcoming invoices for customer")) => {
        -\/(RepositoryError.NoResults("core.services.PaymentServiceDefault.fetchUpcomingInvoiceFromStripe.no.results"))
      }
      case e => -\/(ServiceError.ExternalService(e.toString))
    })
  }

  def listInvoiceItemsFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, List[InvoiceItem]]] = {
    Future successful (try {
      val params = new java.util.HashMap[String, Object]()
      params.put("customer", customerId)

      val invoiceItemList: InvoiceItemCollection = InvoiceItem.list(params, requestOptions)
      val result = invoiceItemList.getData.toList

      \/-(result)
    }
    catch {
      case e => -\/(ServiceError.ExternalService(e.toString))
    })
  }

  def createInvoiceItem(customerId: String, amount: Int, currency: String, description: String = "", metadata: TreeMap[String, Object] = TreeMap.empty): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      invoiceItem <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("customer", customerId)
          params.put("amount", amount.toString)
          params.put("currency", currency)
          params.put("description", description)

          // Generate metadata
          if (metadata.nonEmpty) {
            val initialMetadata = new java.util.HashMap[String, Object]()
            metadata.foreach {
              case (key: String, value: Object) => initialMetadata.put(key, value)
              case _ => ""
            }
            params.put("metadata", initialMetadata)
          }

          val invoiceItem: InvoiceItem = InvoiceItem.create(params, requestOptions)

          \/-(invoiceItem)
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
    } yield Json.parse(APIResource.GSON.toJson(invoiceItem))
  }

  def fetchPaymentInfoFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, Card]] = {
    for {
      paymentInfo <- lift(
        Future successful (try {
          val customer: Customer = Customer.retrieve(customerId, requestOptions)
          val defaultSource = Option(customer.getDefaultSource)

          Option(customer.getSources()) match {
            case Some(sources) if (defaultSource.isDefined) => {
              // Get default payment source
              val defaultSource = customer.getSources().retrieve(customer.getDefaultSource, requestOptions)

              defaultSource.getObject match {
                case "card" => \/-(defaultSource.asInstanceOf[Card])
                case _ => -\/(RepositoryError.NoResults("core.services.PaymentServiceDefault.fetchPaymentInfoFromStripe.no.card"))
              }
            }
            case None => -\/(RepositoryError.NoResults("core.services.PaymentServiceDefault.fetchPaymentInfoFromStripe.no.sources"))
          }
        }
        catch {
          case e => -\/(ServiceError.ExternalService(e.toString))
        })
      )
    } yield paymentInfo
  }

  /**
   * Replace customer payment info (credit card) with a new one in stripe
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

  /**
   * Delete stripe customer's payment info (credit card) from stripe
   *
   * @param customerId
   * @return
   */
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
   * Get a subscription from Stripe by id
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
   * Check if user has access to the application
   *
   * @param userId
   * @return
   */
  def hasAccess(userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      hasAccess <- lift(accountRepository.getByUserId(userId).map {
        case \/-(account) => \/-(
          if (account.status == AccountStatus.free ||
            account.status == AccountStatus.limited ||
            (account.activeUntil.isDefined && account.activeUntil.get.isAfterNow)) // Active until date is greater then now
            {
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
   * Get a stripe event from Stripe by id
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
   * Get a stripe event from krispii database by Id
   *
   * @param eventId
   * @return
   */
  def getEvent(eventId: String): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      event <- lift(stripeRepository.getEvent(eventId))
    } yield event
  }

  /**
   * Create a stripe event in krispii database
   *
   * @param eventId
   * @param eventType
   * @param event
   * @return
   */
  def createEvent(eventId: String, eventType: String, event: JsValue): Future[\/[ErrorUnion#Fail, JsValue]] = {
    for {
      event <- lift(stripeRepository.createEvent(eventId, eventType, event))
    } yield event
  }

  def listLog(): Future[\/[ErrorUnion#Fail, IndexedSeq[PaymentLog]]] = {
    paymentLogRepository.list()
  }

  def listLog(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[PaymentLog]]] = {
    paymentLogRepository.list(userId)
  }

  def createLog(logType: String, description: String, data: String, userId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, PaymentLog]] = {
    paymentLogRepository.insert(
      PaymentLog(
        logType = logType,
        description = description,
        data = data,
        userId = userId
      )
    )
  }

  private def tagUntagUserBasedOnStatus(userId: UUID, newStatus: String, oldStatus: Option[String] = None): Future[\/[ErrorUnion#Fail, Unit]] = {
    newStatus match {
      // Do nothing if status hasn't been changed
      case someNewStatus if oldStatus.isDefined && oldStatus.get == someNewStatus => Future successful \/-()
      // Untag user when switch to these statuses
      case AccountStatus.limited |
        AccountStatus.canceled |
        AccountStatus.onhold |
        AccountStatus.inactive |
        AccountStatus.error |
        AccountStatus.overdue |
        AccountStatus.paid =>
        {
          (for {
            userTags <- lift(tagRepository.listByEntity(userId, TaggableEntities.user))
            tag <- lift(tagRepository.find(UUID.fromString("73d329b7-503a-4ec0-bf41-499feab64c07"))) // krispii tag, use id, because we can change tag names
            _ <- lift {
              if (userTags.contains(tag)) {
                tagRepository.untag(userId, TaggableEntities.user, tag.name, tag.lang).map {
                  case \/-(success) => \/-(success)
                  case -\/(error: RepositoryError.NoResults) => \/-()
                  case -\/(error) => -\/(error)
                }
              }
              else Future successful \/-(Unit)
            }
          } yield ()).run
        }
      // Tag user when switch to these statuses
      case AccountStatus.free |
        AccountStatus.group |
        AccountStatus.trial =>
        {
          (for {
            userTags <- lift(tagRepository.listByEntity(userId, TaggableEntities.user))
            tag <- lift(tagRepository.find(UUID.fromString("73d329b7-503a-4ec0-bf41-499feab64c07"))) // krispii tag, use id, because we can change tag names
            _ <- lift {
              if (!userTags.contains(tag)) {
                tagRepository.tag(userId, TaggableEntities.user, tag.name, tag.lang).map {
                  case \/-(success) => \/-(success)
                  case -\/(RepositoryError.PrimaryKeyConflict) => \/-()
                  case -\/(error) => -\/(error)
                }
              }
              else Future successful \/-(Unit)
            }
          } yield ()).run
        }
      case _ => Future successful \/-(Unit)
    }
  }
}

