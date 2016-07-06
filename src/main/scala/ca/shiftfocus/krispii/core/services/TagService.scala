package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalaz.\/

trait TagService extends Service[ErrorUnion#Fail] {

  /************************************ Tags******************************************/
  def createTag(name: String, lang: String, category: String): Future[\/[ErrorUnion#Fail, Tag]]
  def tag(projectId: UUID, tagName: String): Future[\/[ErrorUnion#Fail, Unit]]
  def untag(projectId: UUID, tagName: String): Future[\/[ErrorUnion#Fail, Unit]]
  def listByKey(key: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listByProjectId(projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listByCategory(category: String, lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def updateTag(name: String, lang: String, category: Option[String]): Future[\/[ErrorUnion#Fail, Tag]]
  def deleteTag(name: String): Future[\/[ErrorUnion#Fail, Tag]]
  /************************************Tag Categories ********************************/
  def createTagCategory(name: String, lang: String): Future[\/[RepositoryError.Fail, TagCategory]]
  def deleteTagCategory(tagCategoryName: String): Future[\/[RepositoryError.Fail, TagCategory]]
  def listTagCategoriesByLanguage(lang: String): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]]
  def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]

}