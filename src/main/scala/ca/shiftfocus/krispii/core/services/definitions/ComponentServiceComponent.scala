package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.Fail
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import scalaz.\/

trait ComponentServiceComponent {
  val componentService: ComponentService

  trait ComponentService {
    def list: Future[\/[Fail, IndexedSeq[Component]]]
    def listByPart(partId: UUID): Future[\/[Fail, IndexedSeq[Component]]]
    def listByProject(projectId: UUID): Future[\/[Fail, IndexedSeq[Component]]]
    def listByProject(projectId: UUID, userId: UUID): Future[\/[Fail, IndexedSeq[Component]]]
    def find(id: UUID): Future[\/[Fail, Component]]

    def createAudio(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, soundCloudId: String): Future[\/[Fail, Component]]
    def createText(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, content: String): Future[\/[Fail, Component]]
    def createVideo(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[\/[Fail, Component]]

    def updateAudio(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[Fail, Component]]
    def updateText(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[Fail, Component]]
    def updateVideo(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[Fail, Component]]

    def delete(id: UUID, version: Long): Future[\/[Fail, Component]]

    def addToPart(componentId: UUID, partId: UUID): Future[\/[Fail, Component]]
    def removeFromPart(componentId: UUID, partId: UUID): Future[\/[Fail, Component]]

    def userCanAccess(component: Component, userInfo: UserInfo): Future[\/[Fail, Component]]

    def detaggify(text: String): String
  }
}
