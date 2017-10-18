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
    val paymentService: PaymentService,
    val config: Configuration
) extends TagService {
  val trialDays = config.getInt("default.trial.days").get
  val defaultStudentLimit = config.getInt("default.student.limit").get
  val defaultStorageLimit = config.getInt("default.storage.limit.gb").get
  val defaultCourseLimit = config.getInt("default.course.limit").get

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  // ########## TAGS ###################################################################################################

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
        tag <- lift(tagRepository.tag(entityId, entityType, existingTag.name, existingTag.lang))
        _ <- lift {
          entityType match {
            case TaggableEntities.user => setUserLimitsByOrganization(entityId, existingTag.name, existingTag.lang)
            case _ => Future successful \/-(Unit)
          }
        }
      } yield tag
    }
  }

  override def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String, shouldUpdateFrequency: Boolean): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        untagged <- lift(tagRepository.untag(entityId, entityType, tagName, tagLang))
        tag <- lift(tagRepository.find(tagName, tagLang))
        frequency = if (tag.frequency - 1 < 0) 0 else (tag.frequency - 1)
        updatedTag <- lift(if (shouldUpdateFrequency) updateFrequency(tag.name, tag.lang, tag.frequency - 1) else Future successful \/-(tag))
        _ <- lift {
          entityType match {
            case TaggableEntities.user => unsetUserLimitsByOrganization(entityId, tag.name, tag.lang)
            case _ => Future successful \/-(Unit)
          }
        }
      } yield untagged
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
      user <- lift(userRepository.find(userId))
      organizations <- lift(organizationRepository.listByTags(IndexedSeq((tagName, tagLang))))
      _ <- lift {
        if (organizations.nonEmpty) {
          val organization = organizations.head
          for {
            limitsJson <- lift(getOrganizationLimits(organization.id))
            // Set limits
            studentLimit = (limitsJson \ "studentLimit").asOpt[Int].getOrElse(0)
            storageLimit = (limitsJson \ "storageLimit").asOpt[Float].getOrElse(0.toFloat)
            courseLimit = (limitsJson \ "courseLimit").asOpt[Int].getOrElse(0)
            dateLimit = (limitsJson \ "dateLimit").asOpt[DateTime].getOrElse(new DateTime)
            courses <- lift(courseRepository.list(user, true))
            _ <- lift(limitRepository.setTeacherStudentLimit(user.id, studentLimit))
            // Delete all custom course student limits
            _ <- lift(serializedT(courses)(course => limitRepository.deleteCourseStudentLimit(course.id)))
            _ <- lift(limitRepository.setStorageLimit(user.id, storageLimit))
            _ <- lift(limitRepository.setCourseLimit(user.id, courseLimit))
            // Get teacher payment account, if there is no account than create one
            account <- lift(paymentService.getAccount(user.id).flatMap {
              case \/-(account) => paymentService.updateAccount(
                account.id,
                account.version,
                AccountStatus.group,
                Some(dateLimit),
                account.customer
              )
              case -\/(error: RepositoryError.NoResults) => {
                (for {
                  newAccount <- lift(paymentService.createAccount(user.id, AccountStatus.group, Some(dateLimit)))
                  log <- lift(paymentService.createLog(PaymentLogType.info, s"Create account for = ${user.email}", Json.toJson(newAccount).toString, Some(newAccount.userId)))
                } yield newAccount).run
              }
              case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
            })
          } yield ()
        }
        else Future successful \/-(Unit)
      }
    } yield ()
  }

  // TODO - Check if there is another organization tag, if so, set limits using it.
  private def unsetUserLimitsByOrganization(userId: UUID, tagName: String, tagLang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    for {
      user <- lift(userRepository.find(userId))
      organizations <- lift(organizationRepository.listByTags(IndexedSeq((tagName, tagLang))))
      _ <- lift {
        if (organizations.nonEmpty) {
          for {
            // Set limits
            _ <- lift(limitRepository.setTeacherStudentLimit(user.id, defaultStudentLimit))
            courses <- lift(courseRepository.list(user, true))
            // Delete all custom course student limits
            _ <- lift(serializedT(courses)(course => limitRepository.deleteCourseStudentLimit(course.id)))
            _ <- lift(limitRepository.setStorageLimit(user.id, defaultStorageLimit))
            _ <- lift(limitRepository.setCourseLimit(user.id, defaultCourseLimit))
            // Get teacher payment account and set activeUntil = user.createdAt + default.trial.days)
            activeUntil = user.createdAt.plusDays(trialDays)
            account <- lift(paymentService.getAccount(user.id).flatMap {
              case \/-(account) => paymentService.updateAccount(
                account.id,
                account.version,
                AccountStatus.trial,
                Some(activeUntil),
                account.customer
              )
              case -\/(error: RepositoryError.NoResults) => Future successful \/-(Unit)
              case -\/(error: ErrorUnion#Fail) => Future successful -\/(error)
            })
          } yield ()
        }
        else Future successful \/-(Unit)
      }
    } yield ()
  }

  private def getOrganizationLimits(organizationId: UUID): Future[\/[ErrorUnion#Fail, JsObject]] = {
    for {
      storageLimit <- lift(limitRepository.getOrganizationStorageLimit(organizationId).map {
        case \/-(limit) => \/-(Some(BigDecimal(limit).setScale(4, BigDecimal.RoundingMode.HALF_UP)))
        case -\/(error: RepositoryError.NoResults) => \/-(None)
        case -\/(error) => -\/(error)
      })
      courseLimit <- lift(limitRepository.getOrganizationCourseLimit(organizationId).map {
        case \/-(limit) => \/-(Some(limit))
        case -\/(error: RepositoryError.NoResults) => \/-(None)
        case -\/(error) => -\/(error)
      })
      studentLimit <- lift(limitRepository.getOrganizationStudentLimit(organizationId).map {
        case \/-(limit) => \/-(Some(limit))
        case -\/(error: RepositoryError.NoResults) => \/-(None)
        case -\/(error) => -\/(error)
      })
      memberLimit <- lift(limitRepository.getOrganizationMemberLimit(organizationId).map {
        case \/-(limit) => \/-(Some(limit))
        case -\/(error: RepositoryError.NoResults) => \/-(None)
        case -\/(error) => -\/(error)
      })
      dateLimit <- lift(limitRepository.getOrganizationDateLimit(organizationId).map {
        case \/-(limit) => \/-(Some(limit))
        case -\/(error: RepositoryError.NoResults) => \/-(None)
        case -\/(error) => -\/(error)
      })
    } yield Json.obj(
      "storageLimit" -> Json.toJson(storageLimit),
      "courseLimit" -> Json.toJson(courseLimit),
      "studentLimit" -> Json.toJson(studentLimit),
      "memberLimit" -> Json.toJson(memberLimit),
      "dateLimit" -> Json.toJson(dateLimit)
    )
  }
}
