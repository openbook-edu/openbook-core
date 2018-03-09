package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.repositories.{ ComponentRepository }
import java.util.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import scalaz.\/

trait ComponentService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val projectService: ProjectService
  val schoolService: SchoolService
  val componentRepository: ComponentRepository

  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def listMasterLimit(limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def listByPart(partId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def listByProject(projectId: UUID, forceAll: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def listByTeacher(teacherId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def find(id: UUID): Future[\/[ErrorUnion#Fail, Component]]

  def createAudio(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, audioData: MediaData, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createImage(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, imageData: MediaData, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createGoogle(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, googleData: MediaData, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createMicrosoft(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, microsoftData: MediaData, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createBook(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, fileData: MediaData, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createText(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, content: String, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createGenericHTML(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, htmlContent: String, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createRubric(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String, rubricContent: String, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]
  def createVideo(id: UUID, ownerId: UUID, title: String, description: String, questions: String, thingsToThinkAbout: String,
    videoData: MediaData, height: Int, width: Int, order: Int, parentId: Option[UUID] = None, parentVersion: Option[Long] = None): Future[\/[ErrorUnion#Fail, Component]]

  def updateAudio(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    audioData: Option[MediaData], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateImage(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    imageData: Option[MediaData], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateGoogle(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    googleData: Option[MediaData], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateMicrosoft(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String], microsoftData: Option[MediaData], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateBook(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    fileData: Option[MediaData], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateText(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    content: Option[String], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateVideo(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String], thingsToThinkAbout: Option[String],
    videoData: Option[MediaData], height: Option[Int], width: Option[Int], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateGenericHTML(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String],
    thingsToThinkAbout: Option[String], htmlContent: Option[String], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]
  def updateRubric(id: UUID, version: Long, ownerId: UUID, title: Option[String], description: Option[String], questions: Option[String],
    thingsToThinkAbout: Option[String], rubricContent: Option[String], order: Option[Int], isPrivate: Option[Boolean], parentId: Option[Option[UUID]] = None, parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]]

  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Component]]

  def addToPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]]
  def removeFromPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]]

  def userCanAccess(component: Component, userInfo: User): Future[\/[ErrorUnion#Fail, Boolean]]

  def detaggify(text: String): String
}
