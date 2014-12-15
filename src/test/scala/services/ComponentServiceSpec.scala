import java.awt.Color

import ca.shiftfocus.uuid.UUID
import webcrank.password.Passwords
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.{PartRepositoryComponent, ProjectRepositoryComponent, UserRepositoryComponent, ComponentRepositoryComponent}
import ca.shiftfocus.krispii.core.services.ComponentServiceImplComponent
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.WordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait ComponentTestEnvironment
  extends ComponentServiceImplComponent
  with ComponentRepositoryComponent
  with UserRepositoryComponent
  with ProjectRepositoryComponent
  with PartRepositoryComponent
  with DB

class ComponentServiceSpec
  extends WordSpec
  with MockFactory
  with ComponentTestEnvironment {
  val logger = Logger[this.type]

  override val userRepository = stub[UserRepository]
  override val componentRepository = stub[ComponentRepository]
  override val projectRepository = stub[ProjectRepository]
  override val partRepository = stub[PartRepository]
  override val db = stub[DBSettings]

  val mockConnection = stub[Connection]
  override def transactional[A](f : Connection => Future[A]): Future[A] = {
    f(mockConnection)
  }

  (db.pool _) when() returns(mockConnection)

  val testAudioComponent = AudioComponent(
    title = "Audio Component title",
    questions = "Audio Component question",
    thingsToThinkAbout = "Audio Component thingsToThinkAbout",
    soundcloudId = "soundcloudId"
  )

  val testTextComponent = TextComponent(
    title = "Text Component title",
    questions = "Text Component question",
    thingsToThinkAbout = "Text Component thingsToThinkAbout",
    content = "Text content"
  )

  val testVideoComponent = VideoComponent(
    title = "Video Component title",
    questions = "Video Component question",
    thingsToThinkAbout = "Video Component thingsToThinkAbout",
    vimeoId = "Vimeo ID",
    width = 640,
    height = 480
  )

  val webcrank = Passwords.scrypt()
  val password = "userpass"
  val passwordHash = webcrank.crypt(password)

  val testUserA = User(
    email = "testUserA@example.org",
    username = "testUserA",
    passwordHash = Some(passwordHash),
    givenname = "Test",
    surname = "UserA"
  )

  val testCourse = Course(
    name = "test course"
  )

  val testClass = Class(
    teacherId = Option(testUserA.id),
    name = "test class",
    color = new Color(24, 6, 8)
  )

  val testProject = Project(
    classId = testClass.id,
    name = "Project name",
    slug = "Project slug",
    description = "Project description",
    parts = IndexedSeq[Part]()
  )

  val testPart = Part(
    projectId = testProject.id,
    name = "Part name",
    position = 1
  )

  implicit val conn = mockConnection

  "ComponentService.createAudio" should {
    inSequence {
      "create audio" in {
        (componentRepository.insert(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testAudioComponent)

        val fAudioComponent = componentService.createAudio(
          testAudioComponent.title,
          testAudioComponent.questions,
          testAudioComponent.thingsToThinkAbout,
          testAudioComponent.soundcloudId
        )

        Await.result(fAudioComponent, Duration.Inf) should be (testAudioComponent)
      }
    }
  }

  "ComponentService.createText" should {
    inSequence {
      "create text" in {
        (componentRepository.insert(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testTextComponent)

        val fTextComponent = componentService.createText(
          testTextComponent.title,
          testTextComponent.questions,
          testTextComponent.thingsToThinkAbout,
          testTextComponent.content
        )

        Await.result(fTextComponent, Duration.Inf) should be (testTextComponent)
      }
    }
  }

  "ComponentService.createVideo" should {
    inSequence {
      "create video" in {
        (componentRepository.insert(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testVideoComponent)

        val fVideoComponent = componentService.createVideo(
          testVideoComponent.title,
          testVideoComponent.questions,
          testVideoComponent.thingsToThinkAbout,
          testVideoComponent.vimeoId,
          testVideoComponent.width,
          testVideoComponent.height
        )

        Await.result(fVideoComponent, Duration.Inf) should be (testVideoComponent)
      }
    }
  }

  "ComponentService.updateAudio" should {
    val values = Map(
      "title" -> "Audio Component new title",
      "questions" -> "Audio Component new questions",
      "thingsToThinkAbout" -> "Audio Component new thingsToThinkAbout",
      "soundcloudId" -> "Audio Component new soundcloudId"
    )

    inSequence {
      "update audio" in {
        (componentRepository.find(_: UUID)(_: Connection)) when(*, mockConnection) returns Future(Option(testAudioComponent))
        (componentRepository.update(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testAudioComponent)

        val fAudioComponent = componentService.updateAudio(
          testAudioComponent.id,
          testAudioComponent.version,
          values
        )

        val result = Await.result(fAudioComponent, Duration.Inf)
        result should be (testAudioComponent)
      }
    }
  }

  "ComponentService.updateText" should {
    val values = Map(
      "title" -> "Text Component new title",
      "questions" -> "Text Component new questions",
      "thingsToThinkAbout" -> "Text Component new thingsToThinkAbout",
      "content" -> "Text Component new content"
    )

    inSequence {
      "update text" in {
        (componentRepository.find(_: UUID)(_: Connection)) when(*, mockConnection) returns Future(Option(testTextComponent))
        (componentRepository.update(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testTextComponent)

        val fTextComponent = componentService.updateText(
          testTextComponent.id,
          testTextComponent.version,
          values
        )

        val result = Await.result(fTextComponent, Duration.Inf)
        result should be (testTextComponent)
      }
    }
  }

  "ComponentService.updateVideo" should {
    val values = Map(
      "title" -> "Video Component new title",
      "questions" -> "Video Component new questions",
      "thingsToThinkAbout" -> "Video Component new thingsToThinkAbout",
      "vimeoId" -> "Video Component new vimeoId",
      "width" -> "Video Component new width",
      "height" -> "Video Component new height"
    )

    inSequence {
      "update video" in {
        (componentRepository.find(_: UUID)(_: Connection)) when(*, mockConnection) returns Future(Option(testVideoComponent))
        (componentRepository.update(_: Component)(_: Connection)) when(*, mockConnection) returns Future.successful(testVideoComponent)

        val fVideoComponent = componentService.updateVideo(
          testVideoComponent.id,
          testVideoComponent.version,
          values
        )

        val result = Await.result(fVideoComponent, Duration.Inf)
        result should be (testVideoComponent)
      }
    }
  }

  "ComponentService.addToPart" should {
    inSequence {
      "add to part" in {
        (componentRepository.find(_: UUID)(_: Connection)) when(*, mockConnection) returns Future(Option(testVideoComponent))
        (partRepository.find(_: UUID)) when(testPart.id) returns Future(Option(testPart))
        (componentRepository.addToPart(_: Component, _: Part)(_: Connection)) when(*, *, mockConnection) returns Future.successful(true)

        val fAddToPart = componentService.addToPart(
          testVideoComponent.id,
          testPart.id
        )

        val result = Await.result(fAddToPart, Duration.Inf)
        result should be (true)
      }
    }
  }

  "ComponentService.removeFromPart" should {
    inSequence {
      "remove from part" in {
        (componentRepository.find(_: UUID)(_: Connection)) when(*, mockConnection) returns Future(Option(testVideoComponent))
        (partRepository.find(_: UUID)) when(testPart.id) returns Future(Option(testPart))
        (componentRepository.removeFromPart(_: Component, _: Part)(_: Connection)) when(*, *, mockConnection) returns Future.successful(true)

        val fRemoveFromPart = componentService.removeFromPart(
          testVideoComponent.id,
          testPart.id
        )

        val result = Await.result(fRemoveFromPart, Duration.Inf)
        result should be (true)
      }
    }
  }
}
