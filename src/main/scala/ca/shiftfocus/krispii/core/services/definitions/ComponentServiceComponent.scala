package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import scalaz.\/

trait ComponentServiceComponent {
  val componentService: ComponentService

  trait ComponentService {
    def list: Future[\/[ServiceError, IndexedSeq[Component]]]
    def listByPart(partId: UUID): Future[\/[ServiceError, IndexedSeq[Component]]]
    def listByProject(projectId: UUID): Future[\/[ServiceError, IndexedSeq[Component]]]
    def listByProject(projectId: UUID, userId: UUID): Future[\/[ServiceError, IndexedSeq[Component]]]
    def find(id: UUID): Future[\/[ServiceError, Component]]

    def createAudio(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, soundCloudId: String): Future[\/[ServiceError, Component]]
    def createText(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, content: String): Future[\/[ServiceError, Component]]
    def createVideo(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[\/[ServiceError, Component]]

    def updateAudio(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[ServiceError, Component]]
    def updateText(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[ServiceError, Component]]
    def updateVideo(id: UUID, version: Long, ownerId: UUID, values: Map[String, Any]): Future[\/[ServiceError, Component]]

    def delete(id: UUID, version: Long): Future[\/[ServiceError, Component]]

    def addToPart(componentId: UUID, partId: UUID): Future[\/[ServiceError, Component]]
    def removeFromPart(componentId: UUID, partId: UUID): Future[\/[ServiceError, Component]]

    def userCanAccess(component: Component, userInfo: UserInfo): Future[\/[ServiceError, Component]]

    def detaggify(text: String): String
  }
}
