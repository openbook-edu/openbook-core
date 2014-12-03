package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait ComponentServiceComponent {
  val componentService: ComponentService

  trait ComponentService {
    def list: Future[IndexedSeq[Component]]
    def list(partId: UUID): Future[IndexedSeq[Component]]
    def list(projectId: UUID, userId: UUID): Future[IndexedSeq[Component]]
    def find(id: UUID): Future[Option[Component]]

    def createAudio(title: String, questions: String, thingsToThinkAbout: String, soundCloudId: String): Future[Component]
    def createText(title: String, questions: String, thingsToThinkAbout: String, content: String): Future[Component]
    def createVideo(title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[Component]

    def updateAudio(id: UUID, version: Long, values: Map[String, Any]): Future[Component]
    def updateText(id: UUID, version: Long, values: Map[String, Any]): Future[Component]
    def updateVideo(id: UUID, version: Long, values: Map[String, Any]): Future[Component]

    def delete(id: UUID, version: Long): Future[Boolean]

    def addToPart(componentId: UUID, partId: UUID): Future[Boolean]
    def removeFromPart(componentId: UUID, partId: UUID): Future[Boolean]

    def detaggify(text: String): String
  }
}
