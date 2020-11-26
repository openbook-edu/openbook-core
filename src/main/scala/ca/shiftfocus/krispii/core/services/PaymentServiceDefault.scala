package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.models.CreditCard
import ca.shiftfocus.krispii.core.models.user.User
// import java.io.{PrintWriter, StringWriter}
import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError, ServiceError}
import ca.shiftfocus.krispii.core.models.stripe.StripePlan
import ca.shiftfocus.krispii.core.models.{Account, AccountStatus, PaymentLog, TaggableEntities}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import com.stripe.exception.InvalidRequestException
import com.stripe.model._
import com.stripe.net.{APIResource, RequestOptions}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaz.{-\/, \/, \/-}

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentServiceDefault(
    val db: DB,
    val requestOptions: RequestOptions,
    val userRepository: UserRepository,
    val accountRepository: AccountRepository,
    val creditCardRepository: CreditCardRepository,
    val stripeRepository: StripeRepository,
    val subscriptionRepository: SubscriptionRepository,
    val paymentLogRepository: PaymentLogRepository,
    val tagRepository: TagRepository,
    val stripePlanRepository: StripePlanRepository
) extends PaymentService {

  implicit def conn: Connection = db.pool

  /**
   * Get locally saved credit card information for an account, or an error
   * @param account: krispii Account
   * @return
   */
  def getCard(account: Account): \/[ErrorUnion#Fail, CreditCard] =
    account.creditCard match {
      case Some(card) => \/-(card)
      case None => -\/(RepositoryError.NoResults("Account has no credit card information"))
    }

  /**
   * Get user account with credit card and stripe plan info from krispii db by userId
   *
   * @param userId: unique ID of krispii user
   * @return
   */
  def getAccount(userId: UUID): Future[\/[ErrorUnion#Fail, Account]] =
    accountRepository.getByUserId(userId)

  /**
   * Get user account with credit card and stripe plan info from krispii db by stripe customer id
   *
   * @param customerId: Stripe customer ID string
   * @return
   */
  def getAccount(customerId: String): Future[\/[ErrorUnion#Fail, Account]] =
    accountRepository.getByCustomerId(customerId)

  /**
   * Create krispii user account in database
   * TODO: on top of the limited account.status add a list of admin tags
   *
   * @param userId: krispii unique user ID
   * @param status String
   * @param activeUntil optional date and time
   * @return a krispii account or an error
   */
  def createAccount(userId: UUID, status: String, activeUntil: Option[DateTime] = None): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      _ <- lift(userRepository.find(userId)) // if there is a constraint on account.user_id existing in users, then this is unnecessary!
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
   * TODO: update list of payment plan tags associated with the subscription
   *
   * @param id
   * @param version How many times the account has already been updated before
   * @param status One of the predefined strings "trial", "limited" etc.
   * @param activeUntil
   * @param overdueStartedAt The Date when overdue period has started TODO for each subscription!
   * @param overdueEndedAt The Date when overdue period has ended TODO for each subscription!
   * @return  Account with subscriptions
   */
  def updateAccount(
    id: UUID,
    version: Long,
    status: String,
    trialStartedAt: Option[Option[DateTime]],
    activeUntil: Option[DateTime],
    overdueStartedAt: Option[Option[DateTime]] = None,
    overdueEndedAt: Option[Option[DateTime]] = None,
    overduePlanId: Option[Option[String]] = None
  ): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      existingAccount <- lift(accountRepository.get(id))
      _ = Logger.debug(s"Account before updating: ${existingAccount}")
      /* UpdateAccountForm in api Payment.scala permits the following behaviors for trialStartedAt:
       - "trialStartedAt":"date" -> Some(Some(date))
       - "trialStartedAt": "" -> Some(None), so delete the field in the database
       - no mention of trialStartedAt -> None, so leave the field as it is
       TODO: make this more logical
       In any case, this bizarre code does not enter into contact with Account.
       */
      updatedAccount <- lift(accountRepository.update(existingAccount.copy(
        status = status,
        trialStartedAt = trialStartedAt match {
        case Some(Some(trialStartedAt)) => Some(trialStartedAt)
        case Some(None) => None
        case None => existingAccount.trialStartedAt
      },
        activeUntil = activeUntil,
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
      // accountRepository.update has ignored subscriptions
      _ = Logger.info(s"In updateAccount, old status for user ${existingAccount.userId} is ${existingAccount.status}, new status is to be ${status}")
      _ <- lift(tagUntagUserBasedOnStatus(updatedAccount.userId, status, Some(existingAccount.status)))
      // subscriptions are stored separate from the rest of the account in the data base
      subscriptions <- lift(subscriptionRepository.listByAccountId(updatedAccount.id))
    } yield updatedAccount.copy(subscriptions = subscriptions) //
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

      val planList: IndexedSeq[JsValue] = Plan.list(params, requestOptions).getData.asScala.map(plan => {
        Json.parse(APIResource.GSON.toJson(plan))
      }).toIndexedSeq

      \/-(planList)
    }
    catch {
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
    }
  }

  /**
   * Get a plan info from stripe by id
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
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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

  /* How to create a JSValue that corresponds to our CreditCard class
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
   */

  private def cardFromStripe(user: User, accountId: UUID, customer: Customer): CreditCard = {
    val defaultSource = customer.getSources.retrieve(customer.getDefaultSource, requestOptions)
    defaultSource.getObject match {
      case "card" =>
        val defaultCard = defaultSource.asInstanceOf[Card]
        CreditCard(customer.getId, version = 1, accountId, user.email, user.givenname, user.surname,
          Some(defaultCard.getExpMonth.toInt), Some(defaultCard.getExpYear.toInt), defaultCard.getBrand, defaultCard.getLast4)
      // not sure other payment sources are of much use to us
      case _ =>
        CreditCard(customer.getId, version = 1, accountId, user.email, user.givenname, user.surname)
    }
  }

  private def readStripeCard(user: User, accountId: UUID, tokenId: String): \/[ErrorUnion#Fail, CreditCard] =
    try {
      val params = new java.util.HashMap[String, Object]()
      params.put("source", tokenId)
      params.put("description", user.givenname + " " + user.surname + " - " + user.id.toString)
      params.put("email", user.email)

      // Customer is a class defined by Stripe
      val customer = Customer.create(params, requestOptions)
      \/-(cardFromStripe(user, accountId, customer))
      // creditCardRepository.insert(creditCard)
    }
    catch {
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
    }

  /**
   * Create CreditCard from stripe information
   *
   * @param account: Account
   * @param tokenId: String
   * @return CreditCard (our subset of stripe's credit card information)
   */
  def createCreditCard(account: Account, tokenId: String): Future[\/[ErrorUnion#Fail, CreditCard]] =
    for {
      user <- lift(userRepository.find(account.userId))
      card <- lift(Future successful readStripeCard(user, account.id, tokenId))
      inserted <- lift(creditCardRepository.insert(card))
    } yield inserted

  /**
   * Create CreditCard from stripe information
   *
   * @param userId unique ID of krispii user
   * @param tokenId String
   * @return CreditCard (our subset of stripe's credit card information)
   */
  def createCreditCard(userId: UUID, tokenId: String): Future[\/[ErrorUnion#Fail, CreditCard]] =
    for {
      user <- lift(userRepository.find(userId))
      account <- lift(accountRepository.getByUserId(userId))
      card <- lift(Future successful readStripeCard(user, account.id, tokenId))
      inserted <- lift(creditCardRepository.insert(card))
    } yield inserted

  private def updateStripeCustomer(userId: UUID, customerId: String, email: String, givenname: String, surname: String): \/[ErrorUnion#Fail, Customer] =
    try {
      val params = new java.util.HashMap[String, Object]()
      params.put("description", s"$givenname $surname-$userId")
      params.put("email", email)

      val customer: Customer = Customer.retrieve(customerId, requestOptions)
      \/-(customer.update(params, requestOptions))
    }
    catch {
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
    }

  /**
   * Update user name and email on stripe, and if successful, also in the locally saved CreditCard.
   * Any changes to the card that occurred in stripe will be synchronized to the krispii db.
   * @param userId unique krispii user ID
   * @param email String
   * @param givenname String
   * @param surname String
   * @return the updated credit card as present in the local database, or an error
   */
  def updateCreditCard(userId: UUID, email: String, givenname: String, surname: String): Future[\/[ErrorUnion#Fail, CreditCard]] =
    (for {
      user <- lift(userRepository.find(userId))
      account <- lift(getAccount(userId))
      card <- lift(Future successful getCard(account))
      stripe <- lift(Future successful updateStripeCustomer(userId, card.customerId, email, givenname, surname))
      toUpdate = cardFromStripe(user, account.id, stripe)
      updated <- lift(creditCardRepository.update(toUpdate))
    } yield updated).run

  /**
   * Guarantee the credit card information in the krispii db is the same as on stripe
   *
   * @param customerId: stripe customer ID string
   * @return
   */
  def updateCreditCardFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, CreditCard]] =
    try {
      val customer: Customer = Customer.retrieve(customerId, requestOptions)
      for {
        account <- lift(getAccount(customerId))
        user <- lift(userRepository.find(account.userId))
        card = cardFromStripe(user, account.id, customer)
        updated <- lift(creditCardRepository.update(card))
      } yield updated
    }
    catch {
      case error: Throwable => Future successful -\/(ServiceError.ExternalService(error.toString))
    }

  private def deleteCustomerFromStripe(customerId: String, requestOptions: RequestOptions): Future[ServiceError.ExternalService \/ Unit] =
    try {
      val customerObj = Customer.retrieve(customerId, requestOptions)
      customerObj.delete(requestOptions)
      Future successful \/-(())
    }
    catch {
      case error: Throwable => Future successful -\/(ServiceError.ExternalService(error.toString))
    }

  /**
   * Delete customer from our DB and from Stripe
   *
   * @param customerId stripe-supplied ID string
   * @return the deleted CreditCard, or an error
   */
  def deleteCustomer(customerId: String): Future[ErrorUnion#Fail \/ CreditCard] =
    (for {
      account <- lift(accountRepository.getByCustomerId(customerId))
      _ <- lift(deleteCustomerFromStripe(customerId, requestOptions))
      deleted <- lift(creditCardRepository.delete(customerId))
    } yield deleted).run

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
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
      updatedSubscription <- lift(
        Future successful (try {
          val params = new java.util.HashMap[String, Object]()
          params.put("plan", newPlanId)

          val subscription: Subscription = Subscription.retrieve(subscriptionId, requestOptions)
          val currentPlan: Plan = subscription.getPlan
          val updatedSubscription = subscription.update(params, requestOptions)
          val newPlan: Plan = updatedSubscription.getPlan

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

          \/-(updatedSubscription)
        }
        catch {
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
        })
      )
      _ <- lift(stripeRepository.updateSubscription(userId, updatedSubscription.getId, Json.parse(APIResource.GSON.toJson(updatedSubscription))))
    } yield Json.parse(APIResource.GSON.toJson(updatedSubscription))
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
      updatedSubscription <- lift(stripeRepository.updateSubscription(userId, subscriptionId, subscription))
    } yield updatedSubscription
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
      canceledSubscription <- lift(
        Future successful (try {
          val subscription: Subscription = Subscription.retrieve(subscriptionId, requestOptions)
          val canceledSubscription = {
            if (atPeriodEnd) {
              val params = new java.util.HashMap[String, Object]()
              params.put("at_period_end", "true")
              subscription.cancel(params, requestOptions)
            }
            else {
              subscription.cancel(null, requestOptions)
            }
          }

          \/-(canceledSubscription)
        }
        catch {
          case e: Throwable => {
            -\/(ServiceError.ExternalService(e.toString))
          }
        })
      )
      _ <- (
        if (atPeriodEnd) {
          lift(stripeRepository.updateSubscription(userId, canceledSubscription.getId, Json.parse(APIResource.GSON.toJson(canceledSubscription))))
        }
        else {
          lift(stripeRepository.deleteSubscription(userId, canceledSubscription.getId))
        }
      )
    } yield Json.parse(APIResource.GSON.toJson(canceledSubscription))
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
      deletedSubscription <- lift(stripeRepository.deleteSubscription(userId, subscriptionId))
    } yield deletedSubscription
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
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
    })
  }

  def listInvoiceItemsFromStripe(customerId: String): Future[\/[ErrorUnion#Fail, List[InvoiceItem]]] = {
    Future successful (try {
      val params = new java.util.HashMap[String, Object]()
      params.put("customer", customerId)

      val invoiceItemList: InvoiceItemCollection = InvoiceItem.list(params, requestOptions)
      val result = invoiceItemList.getData.asScala.toList

      \/-(result)
    }
    catch {
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
          case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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
          account.status == AccountStatus.free ||
            account.status == AccountStatus.limited ||
            (account.activeUntil.isDefined && account.activeUntil.get.isAfterNow) // Active until date is greater then now
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
      case e: Throwable => -\/(ServiceError.ExternalService(e.toString))
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

  /**
   * Remove subscription tags from users who have lost their subscription status.
   *
   * Putting user on limited, inactive or overdue status leads to removal of ALL admin tags.
   * This would be a problem if the user was member of more than one organization with different permissions!
   * TODO: each subscription/group membership has its own expiry status.
   *
   * Putting user on free, group or trial status leads to automatic addition of the "Trial" tag.
   *
   * @param userId
   * @param newStatus
   * @param oldStatus
   * @return: Nothing or error message
   */
  private def tagUntagUserBasedOnStatus(userId: UUID, newStatus: String, oldStatus: Option[String] = None): Future[\/[ErrorUnion#Fail, Unit]] = {
    // val sw = new StringWriter
    // val st = new RuntimeException
    // st.printStackTrace(new PrintWriter(sw))
    // Logger.debug(sw.toString)
    Logger.info(s"In tagUntagUserBasedOnStatus, old status for user ${userId} is ${oldStatus}, new status is ${newStatus}")
    /* Logger.debug(s"In tagUntagUserBasedOnStatus, old status for user ${userId} is ${oldStatus}, new status is ${newStatus} when called in stack..." +
        Thread.currentThread.getStackTrace.filter(trElem => {
        (trElem.toString contains "krispii")
      }).mkString("\n...", "\n...", "")) */
    newStatus match {
      // Do nothing if status hasn't been changed
      case someNewStatus if oldStatus.isDefined && oldStatus.get == someNewStatus => Future successful \/-((): Unit)
      // Untag user when switch to these statuses
      case AccountStatus.limited |
        AccountStatus.inactive |
        AccountStatus.overdue =>
        {
          // | AccountStatus.paid =>
          // We don't need to do anything in case of these statuses:
          //         AccountStatus.canceled |
          //         AccountStatus.onhold |
          //         AccountStatus.error |
          val futureResult = for {
            removeTags <- lift(tagRepository.listAdminByEntity(userId, TaggableEntities.user))
            _ = Logger.info(s"Admin tags to be removed from user with id ${userId} because subscription or trial expired: ${removeTags}")
            _ <- lift(serializedT(removeTags)(tag => {
              Logger.info(s"Removing tag ${tag.name}")
              tagRepository.untag(userId, TaggableEntities.user, tag.name, tag.lang).map {
                case \/-(success) => \/-(success)
                case -\/(error: RepositoryError.NoResults) => \/-((): Unit)
                case -\/(error) => -\/(error)
              }
            }))
          } yield ()
          futureResult.run
        }
      /* ProjectBuilderTag, trialTag, krispiiTag, and/or sexEdTag are given in krispii-api Payment.scala tagUserAccordingPlan
         (for stripe payment) resp. copyOrgAdminSettings (for members recruited by an orgadmin).
         trialTag need only be given to free and trial, but it won't hurt to give it to paid.group, too. */
      case AccountStatus.free |
        AccountStatus.group |
        AccountStatus.trial =>
        {
          val futureResult = for {
            userTags <- lift(tagRepository.listByEntity(userId, TaggableEntities.user))
            // tag <- lift(tagRepository.find(UUID.fromString("73d329b7-503a-4ec0-bf41-499feab64c07"))) // krispii tag, use id, because we can change tag names
            trialTag <- lift(tagRepository.find(UUID.fromString("29479566-3a50-4d36-bc20-f45ff4d4b2d4"))) // all users get this tag to access trial material
            _ <- lift {
              if (!userTags.contains(trialTag)) {
                Logger.info(s"Trying to give trial tag to user with ID ${userId}")
                tagRepository.tag(userId, TaggableEntities.user, trialTag.name, trialTag.lang).map {
                  case \/-(success) => \/-(success)
                  case -\/(RepositoryError.PrimaryKeyConflict) => \/-((): Unit)
                  case -\/(error) => -\/(error)
                }
              }
              else Future successful \/-((): Unit)
            }
          } yield ()
          futureResult.run
        }
      case _ => Future successful \/-((): Unit)
    }
  }
}

