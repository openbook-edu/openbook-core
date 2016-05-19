package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID

import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ -\/, \/-, \/ }

class ComponentServiceDefault(
  val db: DB,
  val scalaCache: ScalaCachePool,
  val authService: AuthService,
  val projectService: ProjectService,
  val schoolService: SchoolService,
  val componentRepository: ComponentRepository,
  val userRepository: UserRepository
)
    extends ComponentService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /**
   * List all components.
   *
   * @return an array of components
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    componentRepository.list(db.pool)
  }

  /**
   * List components by part ID.
   *
   * @param partId the unique ID of the part to filter by
   * @return an array of components
   */
  override def listByPart(partId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      part <- lift(projectService.findPart(partId, false))
      componentList <- lift(componentRepository.list(part))
    } yield componentList
  }

  /**
   * List components by project and user, thus listing "enabled" components.
   *
   * A user can view components that are enabled in any of their courses.
   *
   * @param projectId the unique ID of the part to filter by
   * @return an array of components
   */
  override def listByProject(projectId: UUID, forceAll: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      project <- lift(projectService.find(projectId))
      componentList <- if (forceAll) lift { componentRepository.list(project) }
      else liftSeq {
        Future.sequence(project.parts.filter(_.enabled).map(componentRepository.list(_: Part)))
      }.map(_.flatten.distinct)
    } yield componentList
  }

  /**
   * List all components for a specific teacher
   *
   * @param teacherId
   * @return
   */
  override def listByTeacher(teacherId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      teacher <- lift(userRepository.find(teacherId))
      componentList <- lift(componentRepository.list(teacher))
    } yield componentList
  }

  /**
   * Find a single component by ID.
   *
   * @param id the unique ID of the component to load.
   * @return an optional component
   */
  override def find(id: UUID): Future[\/[ErrorUnion#Fail, Component]] = {
    componentRepository.find(id)
  }

  override def createAudio(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    audioData: MediaData,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = AudioComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      data = audioData,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createBook(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    fileData: MediaData,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = BookComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      data = fileData,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createText(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    content: String,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = TextComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      content = content,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createGenericHTML(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    htmlContent: String,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = GenericHTMLComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      htmlContent = htmlContent,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createRubric(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    rubricContent: String,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = RubricComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      rubricContent = rubricContent,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createVideo(
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    videoData: MediaData,
    height: Int,
    width: Int,
    order: Int
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = VideoComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      data = videoData,
      width = width,
      height = height,
      order = order
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def updateAudio(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    audioData: Option[MediaData],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[AudioComponent])(ServiceError.BadInput("Component type is not audio"))
        existingAudio = existingComponent.asInstanceOf[AudioComponent]
        componentToUpdate = existingAudio.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingAudio.title),
          questions = questions.getOrElse(existingAudio.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingAudio.thingsToThinkAbout),
          data = audioData.getOrElse(existingAudio.data),
          order = order.getOrElse(existingAudio.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateBook(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    fileData: Option[MediaData],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[BookComponent])(ServiceError.BadInput("Component type is not book"))
        existingBook = existingComponent.asInstanceOf[BookComponent]
        componentToUpdate = existingBook.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingBook.title),
          questions = questions.getOrElse(existingBook.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingBook.thingsToThinkAbout),
          data = fileData.getOrElse(existingBook.data),
          order = order.getOrElse(existingBook.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateText(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    content: Option[String],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[TextComponent])(ServiceError.BadInput("Component type is not text"))
        existingText = existingComponent.asInstanceOf[TextComponent]
        componentToUpdate = existingText.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingText.title),
          questions = questions.getOrElse(existingText.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingText.thingsToThinkAbout),
          content = content.getOrElse(existingText.content),
          order = order.getOrElse(existingText.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateGenericHTML(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    htmlContent: Option[String],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[GenericHTMLComponent])(ServiceError.BadInput("Component type is not text"))
        existingGenericHTML = existingComponent.asInstanceOf[GenericHTMLComponent]
        componentToUpdate = existingGenericHTML.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingGenericHTML.title),
          questions = questions.getOrElse(existingGenericHTML.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingGenericHTML.thingsToThinkAbout),
          htmlContent = htmlContent.getOrElse(existingGenericHTML.htmlContent),
          order = order.getOrElse(existingGenericHTML.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateRubric(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    rubricContent: Option[String],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[RubricComponent])(ServiceError.BadInput("Component type is not text"))
        existingRubric = existingComponent.asInstanceOf[RubricComponent]
        componentToUpdate = existingRubric.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingRubric.title),
          questions = questions.getOrElse(existingRubric.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingRubric.thingsToThinkAbout),
          rubricContent = rubricContent.getOrElse(existingRubric.rubricContent),
          order = order.getOrElse(existingRubric.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateVideo(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    videoData: Option[MediaData],
    width: Option[Int],
    height: Option[Int],
    order: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[VideoComponent])(ServiceError.BadInput("Component type is not video"))
        existingVideo = existingComponent.asInstanceOf[VideoComponent]
        componentToUpdate = existingVideo.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingVideo.title),
          questions = questions.getOrElse(existingVideo.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingVideo.thingsToThinkAbout),
          data = videoData.getOrElse(existingVideo.data),
          width = width.getOrElse(existingVideo.width),
          height = height.getOrElse(existingVideo.height),
          order = order.getOrElse(existingVideo.order)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        component <- lift(componentRepository.find(id))
        _ <- predicate(component.version == version)(ServiceError.OfflineLockFail)
        toDelete = component match {
          case comp: AudioComponent => comp.copy(version = version)
          case comp: BookComponent => comp.copy(version = version)
          case comp: TextComponent => comp.copy(version = version)
          case comp: GenericHTMLComponent => comp.copy(version = version)
          case comp: RubricComponent => comp.copy(version = version)
          case comp: VideoComponent => comp.copy(version = version)
        }
        deleted <- lift(componentRepository.delete(toDelete))
      } yield deleted
    }
  }

  /**
   * Add a component to a specific part.
   *
   * This associates a component with a project part. When that part is
   * enabled for a course, users in that course will be able to
   * access that component in their enabled components list.
   *
   * @param componentId the unique ID of the component to add
   * @param partId the unique ID of the part to add the component to
   */
  override def addToPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      val fComponent = componentRepository.find(componentId)
      val fPart = projectService.findPart(partId, false)

      for {
        component <- lift(fComponent)
        part <- lift(fPart)
        addedComp <- lift(componentRepository.addToPart(component, part))
      } yield component
    }
  }

  /**
   * Remove a component from a specific part.
   *
   * This disassociates a component with a project part. When that part is
   * enabled for a course, users in that course will be able to
   * access that component in their enabled components list.
   *
   * @param componentId the unique ID of the component to remove
   * @param partId the unique ID of the part to remove the component from
   * @return the operation was successful
   */
  override def removeFromPart(componentId: UUID, partId: UUID): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      val fComponent = componentRepository.find(componentId)
      val fPart = projectService.findPart(partId, false)

      for {
        component <- lift(fComponent)
        part <- lift(fPart)
        wasRemoved <- lift(componentRepository.removeFromPart(component, part))
      } yield component
    }
  }

  // TODO - this method does nothing
  /**
   * Replace "tags" in a text block with the approriate HTML embed code.
   *
   * Scans text for tags, and then replaces them with the appropriate components.
   *
   * @param text
   * @return
   */
  override def detaggify(text: String): String = {
    //text.r.findAllIn("\[media=([0-9a-fA-F\-])\]")
    text
  }

  /**
   * Can a given user access a given component.
   *
   * @return
   */
  override def userCanAccess(component: Component, user: User): Future[\/[ErrorUnion#Fail, Boolean]] = {
    // Admins can view everything
    if (user.roles.map(_.name).contains("administrator") || component.ownerId == user.id) {
      Future successful \/-(true)
    }
    else {
      // Teachers can view the component if it's in one of their projects
      if (user.roles.map(_.name).contains("teacher")) {

        val fAsTeacher: Future[\/[ErrorUnion#Fail, Boolean]] = for {
          courses <- lift(schoolService.listCoursesByTeacher(user.id))
          projects <- liftSeq(courses.map { course => projectService.list(course.id) })
          components <- liftSeq(projects.flatten.map { project => listByProject(project.id) })
        } yield components.flatten.contains(component)

        fAsTeacher.flatMap {
          case -\/(error) => Future successful -\/(error)
          case \/-(asTeacher) =>
            if (asTeacher) {
              Future successful \/-(true)
            }
            else {
              // Teachers are also students:
              // - list their courses
              // - then list their projects for those courses
              // - then list the components for the enabled parts in those projects
              val fAsStudent: Future[\/[ErrorUnion#Fail, Boolean]] = for {
                courses <- lift(schoolService.listCoursesByUser(user.id))
                projects <- liftSeq(courses.map { course => projectService.list(course.id) })
                components <- liftSeq(projects.flatten.map { project => listByProject(project.id) })
              } yield components.flatten.contains(component)
              fAsStudent
            }
        }
      } // Students can view the component if it's in one of their projects and attached to an active part
      else if (user.roles.map(_.name).contains("student")) {
        for {
          courses <- lift(schoolService.listCoursesByUser(user.id))
          projects <- liftSeq(courses.map { course => projectService.list(course.id) })
          components <- liftSeq(projects.flatten.map { project => listByProject(project.id) })
        } yield components.flatten.contains(component)
      }
      else {
        Future successful \/-(false)
      }
    }
  }
}
