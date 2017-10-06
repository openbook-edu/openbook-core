package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalaz.\/

trait TagService extends Service[ErrorUnion#Fail] {

  /************************************ Tags******************************************/
  def tag(entityId: UUID, entityType: String, tagName: String, lang: String): Future[\/[ErrorUnion#Fail, Unit]]
  def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String, shouldUpdateFrequency: Boolean): Future[\/[ErrorUnion#Fail, Unit]]
  def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]

  def findTag(tagId: UUID): Future[\/[ErrorUnion#Fail, Tag]]
  def findTagByName(name: String, lang: String): Future[\/[ErrorUnion#Fail, Tag]]
  def listByKey(key: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listAdminByKey(key: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listAdminByKey(key: String, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listByEntity(entityId: UUID, entityType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]
  def listByCategory(category: String, lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]]

  def createTag(name: String, lang: String, category: Option[String]): Future[\/[ErrorUnion#Fail, Tag]]
  def updateTag(id: UUID, version: Long, isAdmin: Option[Boolean], name: Option[String], lang: Option[String], category: Option[Option[String]]): Future[\/[ErrorUnion#Fail, Tag]]
  def updateFrequency(name: String, lang: String, frequency: Int): Future[\/[ErrorUnion#Fail, Tag]]
  def deleteTag(id: UUID, vesion: Long): Future[\/[ErrorUnion#Fail, Tag]]

  /************************************Tag Categories ********************************/
  def findTagCategory(id: UUID): Future[\/[ErrorUnion#Fail, TagCategory]]
  def findTagCategoryByName(name: String, lang: String): Future[\/[ErrorUnion#Fail, TagCategory]]
  def listTagCategoriesByLanguage(lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[TagCategory]]]

  def createTagCategory(name: String, lang: String): Future[\/[ErrorUnion#Fail, TagCategory]]
  def updateTagCategory(id: UUID, version: Long, name: Option[String]): Future[\/[ErrorUnion#Fail, TagCategory]]
  def deleteTagCategory(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, TagCategory]]
}