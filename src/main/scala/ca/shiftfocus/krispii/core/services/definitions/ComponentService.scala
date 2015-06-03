package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.lib.concurrent.FutureMonad
import ca.shiftfocus.krispii.core.repositories.{PartRepository, ProjectRepository, ComponentRepository}
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
  def listByPart(partId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def listByProject(projectId: UUID, forceAll: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]]
  def find(id: UUID): Future[\/[ErrorUnion#Fail, Component]]

  def createAudio(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, soundCloudId: String): Future[\/[ErrorUnion#Fail, Component]]
  def createText(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, content: String): Future[\/[ErrorUnion#Fail, Component]]
  def createVideo(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[\/[ErrorUnion#Fail, Component]]

  def updateAudio(id: UUID, version: Long, ownerId: UUID, title: Option[String], questions: Option[String], thingsToThinkAbout: Option[String], soundCloudId: Option[String]): Future[\/[ErrorUnion#Fail, Component]]
  def updateText(id: UUID, version: Long, ownerId: UUID, title: Option[String], questions: Option[String], thingsToThinkAbout: Option[String], content: Option[String]): Future[\/[ErrorUnion#Fail, Component]]
  def updateVideo(id: UUID, version: Long, ownerId: UUID, title: Option[String], questions: Option[String], thingsToThinkAbout: Option[String], vimeoId: Option[String], height: Option[Int], width: Option[Int]): Future[\/[ErrorUnion#Fail, Component]]

  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Component]]

  def addToPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]]
  def removeFromPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]]

  def userCanAccess(component: Component, userInfo: User): Future[\/[ErrorUnion#Fail, Boolean]]

  def detaggify(text: String): String
}