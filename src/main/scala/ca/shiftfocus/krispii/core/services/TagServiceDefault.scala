package ca.shiftfocus.krispii.core.services

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.{ TagCategoryRepository, TagRepository, _ }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class TagServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val tagRepository: TagRepository,
    val tagCategoryRepository: TagCategoryRepository,
    val organizationRepository: OrganizationRepository,
    val limitRepository: LimitRepository,
    val userRepository: UserRepository,
    val courseRepository: CourseRepository,
    val accountRepository: AccountRepository,
    val stripeRepository: StripeRepository,
    val paymentLogRepository: PaymentLogRepository,
    val config: Configuration,
    val projectRepository: ProjectRepository,
    val roleRepository: RoleRepository,
    // Bad idea to include services, but duplicating the code may be even worse
    val paymentService: PaymentService
) extends TagService {
  val trialDays = config.getInt("default.trial.days").get
  val defaultStudentLimit = config.getInt("default.student.limit").get
  val defaultStorageLimit = config.getInt("default.storage.limit.gb").get
  val defaultCourseLimit = config.getInt("default.course.limit").get

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  // ########## TAGS ###################################################################################################

  /**
   * Tag entity with a tag
   * For users if tag is also used in organization, then we set user's limits according to the limits of this organization
   *
   * @param entityId
   * @param entityType
   * @param tagName
   * @param lang
   * @return
   */
  override def tag(entityId: UUID, entityType: String, tagName: String, lang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        existingTag <- lift(
          tagRepository.find(tagName, lang).flatMap {
            case \/-(tag) => Future successful (\/-(tag))
            case -\/(error) => tagRepository.create(Tag(
              name = tagName,
              lang = lang,
              category = None,
              frequency = 0
            ))
          }
        )
        // If entity is already taged with this tag, then do nothing
        _ <- lift(tagRepository.tag(entityId, entityType, existingTag.name, existingTag.lang).map {
          case \/-(success) => \/-(success)
          case -\/(RepositoryError.PrimaryKeyConflict) => \/-()
          case -\/(error) => -\/(error)
        })
        _ <- lift {
          entityType match {
            case TaggableEntities.user => setUserLimitsByOrganization(entityId, existingTag.name, existingTag.lang)
            case _ => Future successful \/-(Unit)
          }
        }
      } yield ()
    }
  }

  /**
   * Untag entity
   * For users: if we want to recalculate user limits, we need to UNTAG USERS FIRST AND ONLY THEN ORGANIZATION.
   * Beacause we check if tag is used in organization, in this case we know that it is organization tag, and we want
   * to change user limits.
   * If user has another organization tag (or tags), we set limits (max limits) from that organization(s).
   *
   * @param entityId
   * @param entityType
   * @param tagName
   * @param tagLang
   * @param shouldUpdateFrequency
   * @return
   */
  override def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String, shouldUpdateFrequency: Boolean): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        _ <- lift {
          // If entity doesn't have this tag, then do nothing
          tagRepository.untag(entityId, entityType, tagName, tagLang).flatMap {
            case \/-(success) => {
              for {
                tag <- lift(tagRepository.find(tagName, tagLang))
                frequency = if (tag.frequency - 1 < 0) 0 else (tag.frequency - 1)
                updatedTag <- lift(if (shouldUpdateFrequency) updateFrequency(tag.name, tag.lang, tag.frequency - 1) else Future successful \/-(tag))
                _ <- lift {
                  entityType match {
                    case TaggableEntities.user => unsetUserLimitsByOrganization(entityId, tag.name, tag.lang)
                    case _ => Future successful \/-(Unit)
                  }
                }
              } yield ()
            }
            case -\/(error: RepositoryError.NoResults) => Future successful \/-()
            case -\/(error) => Future successful -\/(error)
          }
        }
        _ <- lift {
          entityType match {
            case TaggableEntities.project => updateProjectMaster(entityId, tagName, tagLang)
            case _ => Future successful \/-(Unit)
          }
        }
      } yield ()
    }
  }

  override def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    for {
      toClone <- lift(tagRepository.listByEntity(oldProjectId, TaggableEntities.project))
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

  def listByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listByEntity(entityId, entityType)
    }
  }

  def listOrganizationalByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listOrganizationalByEntity(entityId, entityType)
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
    name: Option[String],
    lang: Option[String],
    category: Option[Option[String]]
  ): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(id))
      _ <- predicate(existingTag.version == version)(ServiceError.OfflineLockFail)
      toUpdate = existingTag.copy(
        isAdmin = isAdmin.getOrElse(existingTag.isAdmin),
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
            userTags <- lift(tagRepository.listByEntity(user.id, TaggableEntities.user))
            currentOrganizations <- lift(organizationRepository.listByTags(userTags.map(tag => (tag.name, tag.lang)), false))
            resultOrganizations = (currentOrganizations ++ newOrganizations).distinct
            maxLimitJson <- lift(getOrganizationsMaxLimits(resultOrganizations))
            _ <- lift(setUserLimits(maxLimitJson, user))
          } yield ()
        }
        else Future successful \/-(Unit)
      }
    } yield ()
  }

  /**
   * If deleted user exists then move his account, subscriptions and logs to a new user with the same email
   * @see AuthServiceDefault.syncWithDeletedUser()
   *
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
              // Move subscriptions from old user to a new user
              subscriptions <- lift(stripeRepository.moveSubscriptions(deletedUser.id, newUser.id))
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
            userTags <- lift(tagRepository.listByEntity(user.id, TaggableEntities.user))
            // In case if tag wasn't removed from user, we filter it
            filteredUserTags = userTags.filter(tag => tag.name != tagName || (tag.name == tagName && tag.lang != tagLang))
            remainedOrganizations <- lift(organizationRepository.listByTags(filteredUserTags.map(tag => (tag.name, tag.lang)), false))
            // In case if removed organizations have multiple tags and they still present in the list, we filter them
            filteredRemainedOrganizations = remainedOrganizations.filter(org => !removedOrganizations.contains(org))
            // If user is a part of more then one organization, then we get max limit values from them
            maxLimitJson <- lift(getOrganizationsMaxLimits(filteredRemainedOrganizations))
            _ <- lift(setUserLimits(maxLimitJson, user))
          } yield ()
        }
        else Future successful \/-(Unit)
      }
    } yield ()
  }

  private def setUserLimits(limitsJson: JsObject, user: User): Future[\/[ErrorUnion#Fail, Unit]] = {
    val studentLimit = (limitsJson \ "studentLimit").asOpt[Int].getOrElse(defaultStudentLimit)
    val storageLimit = (limitsJson \ "storageLimit").asOpt[Float].getOrElse(defaultStorageLimit.toFloat)
    val courseLimit = (limitsJson \ "courseLimit").asOpt[Int].getOrElse(defaultCourseLimit)
    val dateLimit = (limitsJson \ "dateLimit").asOpt[DateTime]
    val accountStatus = dateLimit match {
      case Some(activeUntil) => AccountStatus.group
      case _ => AccountStatus.trial
    }

    for {
      _ <- lift(limitRepository.setTeacherStudentLimit(user.id, studentLimit))
      _ <- lift(limitRepository.setStorageLimit(user.id, storageLimit))
      _ <- lift(limitRepository.setCourseLimit(user.id, courseLimit))
      // Delete all custom course student limits
      courses <- lift(courseRepository.list(user, true))
      _ <- lift(serializedT(courses)(course => limitRepository.deleteCourseStudentLimit(course.id)))

      // Get teacher payment account, if there is no account than create one
      account <- lift(paymentService.getAccount(user.id).flatMap {
        case \/-(account) => {
          paymentService.updateAccount(
            account.id,
            account.version,
            accountStatus,
            Some(dateLimit.getOrElse(user.createdAt.plusDays(trialDays))),
            account.customer
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
                Some(dateLimit.getOrElse(user.createdAt.plusDays(trialDays))),
                account.customer
              )
            }
            case -\/(error: RepositoryError.NoResults) => {
              (for {
                newAccount <- lift(paymentService.createAccount(user.id, accountStatus, Some(dateLimit.getOrElse(user.createdAt.plusDays(trialDays)))))
                log <- lift(paymentService.createLog(PaymentLogType.info, s"Create account for = ${user.email}", Json.toJson(newAccount).toString, Some(newAccount.userId)))
              } yield newAccount).run
            }
            case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
          }
        }
        case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
      })
    } yield ()
  }

  // Go threw organization list and get sum of limits values except date limit, here we get max value
  private def getOrganizationsMaxLimits(organizationList: IndexedSeq[Organization]): Future[\/[ErrorUnion#Fail, JsObject]] = {
    for {
      storageLimit <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationStorageLimit(organization.id).map {
          case \/-(limit) => \/-(Some(BigDecimal(limit).setScale(4, BigDecimal.RoundingMode.HALF_UP)))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
        }
      }))
      courseLimit <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationCourseLimit(organization.id).map {
          case \/-(limit) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
        }
      }))
      studentLimit <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationStudentLimit(organization.id).map {
          case \/-(limit) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
        }
      }))
      memberLimit <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationMemberLimit(organization.id).map {
          case \/-(limit) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
        }
      }))
      dateLimit <- lift(serializedT(organizationList)(organization => {
        limitRepository.getOrganizationDateLimit(organization.id).map {
          case \/-(limit) => \/-(Some(limit))
          case -\/(error: RepositoryError.NoResults) => \/-(None)
          case -\/(error) => -\/(error)
        }
      }))
    } yield Json.obj(
      "storageLimit" -> Json.toJson(storageLimit.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }),
      "courseLimit" -> Json.toJson(courseLimit.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }),
      "studentLimit" -> Json.toJson(studentLimit.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }),
      "memberLimit" -> Json.toJson(memberLimit.flatten match {
        case limitList if limitList.nonEmpty => Some(limitList.sum)
        case _ => None
      }),
      "dateLimit" -> Json.toJson(dateLimit.flatten match {
        case limitList if limitList.nonEmpty => {
          implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
          Some(limitList.max)
        }
        case _ => None
      })
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
      teacher <- lift(userRepository.find(course.teacherId))
      teacherRoles <- lift(roleRepository.list(teacher))
      organizationProjectTags <- lift(tagRepository.listOrganizationalByEntity(projectId, TaggableEntities.project))
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
        else Future successful (\/-(Unit))
      }
    } yield ()
  }
}
