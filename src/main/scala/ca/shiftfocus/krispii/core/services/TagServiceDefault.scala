package ca.shiftfocus.krispii.core.services

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError, ServiceError}
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.user.User
import ca.shiftfocus.krispii.core.repositories.{TagCategoryRepository, TagRepository, _}
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

class TagServiceDefault(
    val db: DB,
    val tagRepository: TagRepository,
    val tagCategoryRepository: TagCategoryRepository,
    val organizationRepository: OrganizationRepository,
    val limitRepository: LimitRepository,
    val userRepository: UserRepository,
    val courseRepository: CourseRepository,
    val accountRepository: AccountRepository,
    val stripeRepository: StripeEventRepository,
    val paymentLogRepository: PaymentLogRepository,
    val config: Configuration,
    val projectRepository: ProjectRepository,
    val roleRepository: RoleRepository,
    val userPreferenceRepository: UserPreferenceRepository,
    // Bad idea to include services, but duplicating the code may be even worse
    val paymentService: PaymentService
) extends TagService {
  private val trialDays = config.get[Option[Int]]("default.trial.days").get
  private val defaultStudentLimit = config.get[Option[Int]]("default.student.limit").get
  private val defaultStorageLimit = config.get[Option[Int]]("default.storage.limit.gb").get
  private val defaultCourseLimit = config.get[Option[Int]]("default.course.limit").get

  implicit def conn: Connection = db.pool

  // ########## TAGS ###################################################################################################

  /**
   * Tag entity with a tag
   * For users who are members of this organization, if this is a payment tag (admin, not private content),
   * then we set the users' limits according to the limits of this organization.
   *
   * @param entityId UUID (organization, project or user) or String (Stripe plan)
   * @param entityType String
   * @param tagName String
   * @param lang String
   * @return Future containing an error or Unit
   */
  override def tag(entityId: UUID, entityType: String, tagName: String, lang: String): Future[\/[ErrorUnion#Fail, Unit]] =
    tag(entityId.toString, entityType, tagName, lang)
  override def tag(entityId: String, entityType: String, tagName: String, lang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        existingTag <- lift(
          tagRepository.find(tagName, lang).flatMap {
            case \/-(tag) => Future successful (\/-(tag))
            case -\/(_) =>
              Logger.info(s"Tag $tagName:$lang did not exist, will be created now.")
              tagRepository.create(Tag(
                name = tagName,
                lang = lang,
                category = None,
                frequency = 0
              ))
          }
        )
        // If entity is already tagged with this tag, then do nothing
        _ <- lift(tagRepository.tag(entityId, entityType, existingTag.name, existingTag.lang).map {
          case \/-(success) => \/-(success)
          case -\/(RepositoryError.PrimaryKeyConflict) | -\/(_: RepositoryError.UniqueKeyConflict) =>
            Logger.info(s"$entityType $entityId already has tag $tagName:$lang - do nothing.")
            \/-(())
          case -\/(error) =>
            Logger.error(s"Error while tagging $entityType $entityId with tag $tagName:$lang: $error.")
            -\/(error)
        })
        _ <- lift {
          entityType match {
            case TaggableEntities.user =>
              Logger.debug(s"Now setting limits for user $entityId from the limits of their organization(s)")
              setUserLimitsByOrganization(UUID.fromString(entityId), existingTag.name, existingTag.lang)
            case _ => Future successful \/-(())
          }
        }
      } yield ()
    }
  }

  /**
   * Untag an entity (organization, project, user or Stripe plan).
   * For users: if we want to recalculate user limits, we need to UNTAG USERS FIRST AND ONLY THEN ORGANIZATION.
   * Because we check if tag is used in organization, in this case we know that it is organization tag, and we want
   * to change user limits.
   * If user has another organization tag (or tags), we set limits (max limits) from that organization(s).
   *
   * @param entityId UUID (organization, project or user) or String (Stripe plan)
   * @param entityType String
   * @param tagName String
   * @param tagLang String
   * @param shouldUpdateFrequency Boolean
   * @return Future containing an error or Unit
   */
  override def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String, shouldUpdateFrequency: Boolean): Future[\/[ErrorUnion#Fail, Unit]] =
    untag(entityId.toString, entityType, tagName, tagLang, shouldUpdateFrequency)
  override def untag(entityId: String, entityType: String, tagName: String, tagLang: String, shouldUpdateFrequency: Boolean): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        _ <- lift {
          // If entity doesn't have this tag, then do nothing
          tagRepository.untag(entityId, entityType, tagName, tagLang).flatMap {
            case \/-(_) =>
              for {
                tag <- lift(tagRepository.find(tagName, tagLang))
                frequency = if (tag.frequency - 1 < 0) 0 else (tag.frequency - 1)
                _ <- lift(if (shouldUpdateFrequency) updateFrequency(tag.name, tag.lang, frequency) else Future successful \/-(tag))
                _ <- lift {
                  entityType match {
                    case TaggableEntities.user => unsetUserLimitsByOrganization(UUID.fromString(entityId), tag.name, tag.lang)
                    case _ => Future successful \/-((): Unit)
                  }
                }
              } yield ()
            case -\/(_: RepositoryError.NoResults) => Future successful \/-((): Unit)
            case -\/(error) => Future successful -\/(error)
          }
        }
        _ <- lift {
          entityType match {
            case TaggableEntities.project => updateProjectMaster(UUID.fromString(entityId), tagName, tagLang)
            case _ => Future successful \/-((): Unit)
          }
        }
      } yield ()
    }
  }

  override def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    for {
      toClone <- lift(tagRepository.listByEntity(oldProjectId.toString, TaggableEntities.project))
      cloned <- lift(serializedT(toClone)(tag => {
        for {
          inserted <- lift(tagRepository.create(tag))
        } yield inserted
      }))
    } yield cloned
  }

  def isOrganizational(name: String, lang: String): Future[\/[ErrorUnion#Fail, Boolean]] = {
    tagRepository.isOrganizational(name, lang)
  }

  def findTag(tagId: UUID): Future[\/[ErrorUnion#Fail, Tag]] = {
    tagRepository.find(tagId)
  }

  def findTagByName(name: String, lang: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    tagRepository.find(name, lang)
  }

  def listPopular(lang: String, limit: Int = 0, skipedCategories: IndexedSeq[String] = IndexedSeq.empty[String]): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    tagRepository.listPopular(lang, limit, skipedCategories)
  }

  override def listByKey(key: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional { implicit conn: Connection =>
      tagRepository.trigramSearch(key)
    }
  }

  override def listAdminByKey(key: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional { implicit conn: Connection =>
      tagRepository.trigramSearchAdmin(key)
    }
  }

  override def listAdminByKey(key: String, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional { implicit conn: Connection =>
      tagRepository.trigramSearchAdmin(key, userId)
    }
  }

  def listByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] =
    listByEntity(entityId.toString, entityType)
  def listByEntity(entityId: String, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listByEntity(entityId, entityType)
    }
  }

  def listOrganizationalByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] =
    listOrganizationalByEntity(entityId.toString, entityType)
  def listOrganizationalByEntity(entityId: String, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listOrganizationalByEntity(entityId, entityType)
    }
  }
  // DON'T USE, WILL THROW RUNTIME ERROR AT LEAST FOR ORGANIZATIONS! USE tags.filter(_.isAdmin)
  def listAdminByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] =
    listAdminByEntity(entityId.toString, entityType)
  def listAdminByEntity(entityId: String, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listAdminByEntity(entityId, entityType)
    }
  }

  def listByCategory(category: String, lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listByCategory(category, lang)
    }
  }

  override def createTag(name: String, lang: String, category: Option[String]): Future[\/[ErrorUnion#Fail, Tag]] = {
    transactional { implicit conn: Connection =>
      tagRepository.create(Tag(
        name = name,
        lang = lang,
        category = category,
        frequency = 0
      ))
    }
  }

  override def updateTag(
    id: UUID,
    version: Long,
    isAdmin: Option[Boolean],
    isHidden: Option[Boolean],
    isPrivate: Option[Boolean],
    name: Option[String],
    lang: Option[String],
    category: Option[Option[String]]
  ): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(id))
      _ <- predicate(existingTag.version == version)(ServiceError.OfflineLockFail)
      toUpdate = existingTag.copy(
        isAdmin = isAdmin.getOrElse(existingTag.isAdmin),
        isHidden = isHidden.getOrElse(existingTag.isHidden),
        isPrivate = isPrivate.getOrElse(existingTag.isPrivate),
        name = name.getOrElse(existingTag.name),
        lang = lang.getOrElse(existingTag.lang),
        category = category match {
          case Some(Some(category)) => Some(category)
          case Some(None) => None
          case None => existingTag.category
        }
      )
      updatedTag <- lift(tagRepository.update(toUpdate))
    } yield updatedTag
  }

  override def updateFrequency(name: String, lang: String, frequency: Int): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(name, lang))
      toUpdate = existingTag.copy(frequency = frequency)
      updatedTag <- lift(tagRepository.update(toUpdate))
    } yield updatedTag
  }

  def deleteTag(tagId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(tagId))
      _ <- predicate(existingTag.version == version)(ServiceError.OfflineLockFail)
      deletedTag <- lift(tagRepository.delete(existingTag))
    } yield deletedTag
  }

  // ########## TAG CATEGORIES #########################################################################################

  def findTagCategory(id: UUID): Future[\/[ErrorUnion#Fail, TagCategory]] = {
    tagCategoryRepository.find(id)
  }

  def findTagCategoryByName(name: String, lang: String): Future[\/[ErrorUnion#Fail, TagCategory]] = {
    tagCategoryRepository.findByName(name, lang)
  }

  def listTagCategoriesByLanguage(lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[TagCategory]]] = {
    transactional {
      implicit conn: Connection =>
        tagCategoryRepository.listByLanguage(lang)
    }
  }

  def createTagCategory(name: String, lang: String): Future[\/[ErrorUnion#Fail, TagCategory]] = {
    transactional {
      implicit conn: Connection =>
        tagCategoryRepository.create(TagCategory(
          name = name,
          lang = lang
        ))
    }
  }

  def updateTagCategory(id: UUID, version: Long, name: Option[String]): Future[\/[ErrorUnion#Fail, TagCategory]] = {
    for {
      existingTagCategory <- lift(tagCategoryRepository.find(id))
      _ <- predicate(existingTagCategory.version == version)(ServiceError.OfflineLockFail)
      updatedTagCategory <- lift(tagCategoryRepository.update(existingTagCategory.copy(
        name = name.getOrElse(existingTagCategory.name)
      )))
    } yield updatedTagCategory

  }

  def deleteTagCategory(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, TagCategory]] = {
    for {
      existingTagCategory <- lift(tagCategoryRepository.find(id))
      _ <- predicate(existingTagCategory.version == version)(ServiceError.OfflineLockFail)
      deletedTagCategory <- lift(tagCategoryRepository.delete(existingTagCategory))
    } yield deletedTagCategory
  }

  private def setUserLimitsByOrganization(userId: UUID, tagName: String, tagLang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    for {
      newOrganizations <- lift(organizationRepository.listByTags(IndexedSeq((tagName, tagLang))))
      _ <- lift {
        if (newOrganizations.nonEmpty) {
          for {
            user <- lift(userRepository.find(userId))
            _ = Logger.info(s"Setting limits for ${user.email} who was tagged with $tagName:")
            userTags <- lift(listByEntity(user.id, TaggableEntities.user))
            currentOrganizations <- lift(organizationRepository.listByTags(userTags.map(tag => (tag.name, tag.lang)), false))
            resultOrganizations = (currentOrganizations ++ newOrganizations).distinct
            maxLimitJson <- lift(getOrganizationsMaxLimits(resultOrganizations))
            _ = Logger.info(s"From organization(s) ${resultOrganizations.map(_.title)}, max limits are $maxLimitJson!")
            _ <- lift(setUserLimits(maxLimitJson, user))
            _ <- lift {
              // If user doesn't have organizations, that means he haven't seen welcome popup for organization
              if (currentOrganizations.isEmpty) {
                userPreferenceRepository.set(UserPreference(
                  userId = userId,
                  prefName = "teacher_check",
                  state = "show"
                ))
              }
              else Future successful \/-((): Unit)
            }
          } yield ()
        }
        else Future successful \/-((): Unit)
      }
    } yield ()
  }

  /**
   * If deleted user exists then move his account, subscriptions and logs to a new user with the same email
   * @see AuthServiceDefault.syncWithDeletedUser()
   * TODO: eliminate one of the two!
   * @param newUser
   * @return
   */
  private def syncWithDeletedUser(newUser: User): Future[\/[ErrorUnion#Fail, Account]] = {
    for {
      // Check if user was deleted and has stripe account and subscriptions
      account <- lift(userRepository.findDeleted(newUser.email).flatMap {
        // If deleted user is found
        case \/-(deletedUser) => {
          // We need to check if emails match 100%, because deleted user can be: deleted_1487883998_some.email@example.com,
          // and new user can be email@example.com, which will also match sql LIKE query: '%email@example.com'
          // @see userRepository.delete and userRepository.findDeleted
          // So we need to clean deleted email to compare it
          val oldEmail = deletedUser.email.replaceAll("^deleted_[0-9]{10}_", "")
          val newEmail = newUser.email

          if (oldEmail == newEmail) {
            for {
              // Move account from old user to a new user
              account <- lift(accountRepository.getByUserId(deletedUser.id).flatMap {
                case \/-(account) => accountRepository.update(account.copy(userId = newUser.id))
                case -\/(error) => Future successful -\/(error)
              })
              // Move payment logs from old user to a new user
              paymentLogs <- lift(paymentLogRepository.move(deletedUser.id, newUser.id))
            } yield (account)
          }
          else Future successful -\/(RepositoryError.NoResults("core.TagServiceDefault.syncWithDeletedUser.no.user"))
        }
        case -\/(error) => Future successful -\/(error)
      })
    } yield account
  }

  /**
   * If user still has organization tag(s), we get that organization(s) and set limits (max limits) using it (them).
   * If user now is member of no organzation anymore, status will be set to limited because of the lack of
   * an "active until" date.
   *
   * @param userId
   * @param tagName
   * @param tagLang
   * @return
   */
  private def unsetUserLimitsByOrganization(userId: UUID, tagName: String, tagLang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    for {
      removedOrganizations <- lift(organizationRepository.listByTags(IndexedSeq((tagName, tagLang))))
      _ <- lift {
        if (removedOrganizations.nonEmpty) {
          for {
            user <- lift(userRepository.find(userId))
            userTags <- lift(listByEntity(user.id, TaggableEntities.user))
            // In case if tag wasn't removed from user, we filter it
            filteredUserTags = userTags.filter(tag => tag.name != tagName || (tag.name == tagName && tag.lang != tagLang))
            remainedOrganizations <- lift(organizationRepository.listByTags(filteredUserTags.map(tag => (tag.name, tag.lang)), false))
            // In case if removed organizations have multiple tags and they still present in the list, we filter them
            filteredRemainedOrganizations = remainedOrganizations.filter(org => !removedOrganizations.contains(org))
            // If user is a part of more then one organization, then we get max limit values from them
            maxLimitJson <- lift(getOrganizationsMaxLimits(filteredRemainedOrganizations))
            _ = Logger.info(s"unsetting limits for ${user.email} to ${maxLimitJson}")
            _ <- lift(setUserLimits(maxLimitJson, user))
          } yield ()
        }
        else Future successful \/-((): Unit)
      }
    } yield ()
  }

  private def setUserLimits(limitsJson: JsObject, user: User): Future[\/[ErrorUnion#Fail, Account]] = {
    val studentLimit = (limitsJson \ "studentLimit").asOpt[Int].getOrElse(defaultStudentLimit)
    val storageLimit = (limitsJson \ "storageLimit").asOpt[Float].getOrElse(defaultStorageLimit.toFloat)
    val courseLimit = (limitsJson \ "courseLimit").asOpt[Int].getOrElse(defaultCourseLimit)
    val dateLimit = (limitsJson \ "dateLimit").asOpt[DateTime].getOrElse(user.createdAt.plusDays(trialDays))
    Logger.info(s"in setUserLimit, date limit for ${user.email} is ${dateLimit}")
    /* We will now always set a date limit. When a "trial" or "paid" user passes the date limit,
    they will become "limited". That is better than allowing an absence of date limit, which immediately
    turns them "limited". "Free" users need not really have a date limit.
    TODO: check if createdAt + trial days is already past or is after the limit of the organization */
    val accountStatus = AccountStatus.group

    (for {
      _ <- lift(limitRepository.setTeacherStudentLimit(user.id, studentLimit))
      _ <- lift(limitRepository.setStorageLimit(user.id, storageLimit))
      _ <- lift(limitRepository.setCourseLimit(user.id, courseLimit))
      // Delete all custom group student limits
      courses <- lift(courseRepository.list(user, true))
      _ <- lift(serializedT(courses)(course => limitRepository.deleteCourseStudentLimit(course.id)))

      // Get teacher payment account, if there is no account than create one
      account <- lift(paymentService.getAccount(user.id).flatMap {
        case \/-(account) => {
          paymentService.updateAccount(
            account.id,
            account.version,
            accountStatus,
            None,
            Some(dateLimit)
          )
        }
        case -\/(error: RepositoryError.NoResults) => {
          // Account can exist from times when user was marked as deleted
          syncWithDeletedUser(user).flatMap {
            case \/-(account) => {
              paymentService.updateAccount(
                account.id,
                account.version,
                accountStatus,
                None,
                Some(dateLimit)
              )
            }
            case -\/(error: RepositoryError.NoResults) => {
              (for {
                newAccount <- lift(paymentService.createAccount(user.id, accountStatus, Some(dateLimit)))
                log <- lift(paymentService.createLog(PaymentLogType.info, s"Create account for = ${user.email}", Json.toJson(newAccount).toString, Some(newAccount.userId)))
              } yield newAccount).run
            }
            case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
          }
        }
        case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
      })
    } yield account).run
  }

  // Go through organization list and get sum of limits values except date limit, here we get max value
  private def getOrganizationsMaxLimits(organizationList: IndexedSeq[Organization]): Future[\/[ErrorUnion#Fail, JsObject]] = {
    for {
      storageLimits <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationStorageLimit(organization.id).map {
          case \/-(limit: Float) => \/-(Some(BigDecimal.decimal(limit).setScale(4, BigDecimal.RoundingMode.HALF_UP)))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
          case _ => \/-({ Logger.error(s"Problem with format of storage limits for organization ${organization.title}"); None })
        }
      }))
      courseLimits <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationCourseLimit(organization.id).map {
          case \/-(limit: Int) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
          case _ => \/-({ Logger.error(s"Problem with format of group limits for organization ${organization.title}"); None })
        }
      }))
      studentLimits <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationStudentLimit(organization.id).map {
          case \/-(limit: Int) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
          case _ => \/-({ Logger.error(s"Problem with format of student limits for organization ${organization.title}"); None })
        }
      }))
      memberLimits <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationMemberLimit(organization.id).map {
          case \/-(limit: Int) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
          case _ => \/-({ Logger.error(s"Problem with format of member limits for organization ${organization.title}"); None })
        }
      }))
      dateLimits <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationDateLimit(organization.id).map {
          case \/-(limit: DateTime) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
          case _ => \/-({ Logger.error(s"Problem with format of date limits for organization ${organization.title}"); None })
        }
      }))
    } yield Json.obj(
      "storageLimit" -> {
        (storageLimits.flatten match {
          case limitList if limitList.nonEmpty => Some(limitList.sum)
          case _ => None
        }).flatMap(d => Some(Json.toJson(d).toString()))
      },
      "courseLimit" -> (courseLimits.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }).flatMap(d => Some(Json.toJson(d).toString())),
      "studentLimit" -> (studentLimits.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }).flatMap(d => Some(Json.toJson(d).toString())),
      "memberLimit" -> (memberLimits.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }).flatMap(d => Some(Json.toJson(d).toString())),
      "dateLimit" -> (dateLimits.flatten match {
        case limitList if limitList.nonEmpty => {
          implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
          Some(limitList.max)
        }
        case _ => None
        // }).flatMap(d => Some(Json.toJson(d).toString()))
      }).flatMap(d => Some(Json.toJson(d))) // looks like it already was a String
    )
  }

  /**
   * If we untag last organization tag from a project, and project owner is not manager, then we set project isMaster = false
   *
   * @param projectId
   * @param tagName
   * @param tagLang
   * @return
   */
  private def updateProjectMaster(projectId: UUID, tagName: String, tagLang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    for {
      project <- lift(projectRepository.find(projectId))
      course <- lift(courseRepository.find(project.courseId))
      teacher <- lift(userRepository.find(course.ownerId))
      teacherRoles <- lift(roleRepository.list(teacher))
      organizationProjectTags <- lift(tagRepository.listOrganizationalByEntity(projectId.toString, TaggableEntities.project))
      _ <- lift {
        // Check if user tries to untag the last organization tag
        // If project doesn't belong to a manager (but to a orgManger), then we set isMaster = false
        if (!teacherRoles.map(_.name).contains("manager") &&
          project.isMaster &&
          organizationProjectTags.length == 1 &&
          organizationProjectTags.head.name == tagName &&
          organizationProjectTags.head.lang == tagLang) {
          projectRepository.update(project.copy(isMaster = false))
        }
        else Future successful (\/-((): Unit))
      }
    } yield ()
  }
}
