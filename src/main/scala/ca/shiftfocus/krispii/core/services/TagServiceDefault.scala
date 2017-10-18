package ca.shiftfocus.krispii.core.services

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.models.{ Tag, TagCategory, TaggableEntities }
import ca.shiftfocus.krispii.core.repositories.{ TagCategoryRepository, TagRepository, _ }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class TagServiceDefault(
    val db: DB,
    val tagRepository: TagRepository,
    val tagCategoryRepository: TagCategoryRepository
) extends TagService {

  implicit def conn: Connection = db.pool

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
}
