package ca.shiftfocus.krispii.core.services

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError }
import ca.shiftfocus.krispii.core.models.{ Tag, TagCategory }
import ca.shiftfocus.krispii.core.repositories.{ TagCategoryRepository, TagRepository, _ }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class TagServiceDefault(
    val db: DB,
    val tagRepository: TagRepository,
    val tagCategoryRepository: TagCategoryRepository
) extends TagService {

  implicit def conn: Connection = db.pool

  override def createTag(name: String, lang: String, category: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    transactional { implicit conn: Connection =>
      tagRepository.create(Tag(name, lang, category))
    }
  }
  override def tag(projectId: UUID, tagName: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      tagRepository.tag(projectId, tagName)
    }
  }

  override def untag(projectId: UUID, tagName: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      tagRepository.untag(projectId, tagName)
    }
  }

  override def listByKey(key: String): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    transactional { implicit conn: Connection =>
      tagRepository.trigramSearch(key)
    }
  }

  def listByProjectId(projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listByProjectId(projectId)
    }
  }
  def listByCategory(category: String, lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    transactional {
      implicit conn: Connection =>
        tagRepository.listByCategory(category, lang)
    }
  }

  override def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    for {
      toClone <- lift(tagRepository.listByProjectId(oldProjectId))
      cloned <- lift(serializedT(toClone)(tag => {
        for {
          inserted <- lift(tagRepository.create(tag))
        } yield inserted
      }))
    } yield cloned
  }

  override def updateTag(name: String, lang: String, category: Option[String]): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(name))
      toUpdate = existingTag.copy(name = existingTag.name, lang = lang, category = category.getOrElse(existingTag.category))
      updatedTag <- lift(tagRepository.update(toUpdate))
    } yield updatedTag
  }

  def deleteTag(name: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    tagRepository.delete(name)
  }

  def createTagCategory(name: String, lang: String): Future[\/[RepositoryError.Fail, TagCategory]] = {
    transactional {
      implicit conn: Connection =>
        tagCategoryRepository.create(TagCategory(name, lang))
    }
  }
  def deleteTagCategory(tagCategoryName: String): Future[\/[RepositoryError.Fail, TagCategory]] = {
    transactional {
      implicit conn: Connection =>
        tagCategoryRepository.delete(tagCategoryName)
    }
  }
  def listTagCategoriesByLanguage(lang: String): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]] = {
    transactional {
      implicit conn: Connection =>
        tagCategoryRepository.listByLanguage(lang)
    }
  }
}
