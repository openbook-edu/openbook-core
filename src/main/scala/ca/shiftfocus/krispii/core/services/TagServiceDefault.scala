package ca.shiftfocus.krispii.core.services

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.{ ErrorUnion, RepositoryError }
import ca.shiftfocus.krispii.core.models.{ Tag, TagCategory }
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

  override def createTag(name: String, lang: String, category: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    transactional { implicit conn: Connection =>
      tagRepository.create(Tag(
        name = name,
        lang = lang,
        category = category,
        frequency = 0
      ))
    }
  }
  override def tag(projectId: UUID, tagName: String, lang: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        existingTag <- lift(
          tagRepository.find(tagName, lang).flatMap {
            case \/-(tag) => Future successful (\/-(tag))
            case -\/(error) => tagRepository.create(Tag(
              name = tagName,
              lang = lang,
              category = "",
              frequency = 0
            ))
          }
        )
        tag <- lift(tagRepository.tag(projectId, existingTag.name, existingTag.lang))
      } yield tag
    }
  }

  override def untag(projectId: UUID, tagName: String, tagLang: String, projectState: Boolean): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      for {
        untagged <- lift(tagRepository.untag(projectId, tagName, tagLang))
        tag <- lift(tagRepository.find(tagName, tagLang))
        frequency = if (tag.frequency - 1 < 0) 0 else (tag.frequency - 1)
        updatedTag <- lift(if (projectState) updateFrequency(tag.name, tag.lang, tag.frequency - 1) else Future successful \/-(tag))
      } yield untagged
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

  override def updateFrequency(name: String, lang: String, frequency: Int): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(name, lang))
      toUpdate = existingTag.copy(name = existingTag.name, lang = existingTag.lang, frequency = frequency)
      updatedTag <- lift(tagRepository.update(toUpdate))
    } yield updatedTag
  }
  override def updateTag(name: String, lang: String, category: Option[String]): Future[\/[ErrorUnion#Fail, Tag]] = {
    for {
      existingTag <- lift(tagRepository.find(name, lang))
      toUpdate = existingTag.copy(name = existingTag.name, lang = existingTag.lang, category = category.getOrElse(existingTag.category))
      updatedTag <- lift(tagRepository.update(toUpdate))
    } yield updatedTag
  }

  def deleteTag(name: String, lang: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    tagRepository.delete(name, lang)
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

  def findTagByName(name: String, lang: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    tagRepository.find(name, lang)
  }

}
