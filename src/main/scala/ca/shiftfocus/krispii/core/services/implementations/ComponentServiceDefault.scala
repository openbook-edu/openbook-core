package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

class ComponentServiceDefault(val db: Connection,
                              val authService: AuthService,
                              val projectService: ProjectService,
                              val schoolService: SchoolService,
                              val componentRepository: ComponentRepository)
  extends ComponentService {

  implicit def conn: Connection = db

  /**
   * List all components.
   *
   * @return an array of components
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    componentRepository.list(db)
  }

  /**
   * List components by part ID.
   *
   * @param partId the unique ID of the part to filter by
   * @return an array of components
   */
  override def listByPart(partId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      part <- lift(projectService.findPart(partId))
      componentList <- lift(componentRepository.list(part))
    }
    yield componentList
  }

  /**
   * List components by project and user, thus listing "enabled" components.
   *
   * A user can view components that are enabled in any of their courses.
   *
   * @param projectId the unique ID of the part to filter by
   * @return an array of components
   */
  override def listByProject(projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      project <- lift(projectService.find(projectId))
      componentList <- lift(componentRepository.list(project))
    }
    yield componentList
  }

  /**
   * List components by project and user, thus listing "enabled" components.
   *
   * A user can view components that are enabled in any of their courses.
   *
   * @param projectId the unique ID of the part to filter by
   * @param userId the user to check for
   * @return an array of components
   */
  override def listByProject(projectId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    val fProject = projectService.find(projectId)
    val fUser = authService.find(userId)

    for {
      project <- lift(fProject)
      user <- lift(fUser)
      componentList <- lift(componentRepository.list(project, user))
    }
    yield componentList
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

  override def createAudio(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, soundcloudId: String): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = AudioComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      soundcloudId = soundcloudId
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createText(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, content: String): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = TextComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      content = content
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createVideo(ownerId: UUID, title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = VideoComponent(
      ownerId = ownerId,
      title = title,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      vimeoId = vimeoId,
      width = width,
      height = height
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def updateAudio(id: UUID, version: Long, ownerId: UUID,
                           title: Option[String],
                           questions: Option[String],
                           thingsToThinkAbout: Option[String],
                           soundcloudId: Option[String]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        existingAudio = existingComponent.asInstanceOf[AudioComponent]
        componentToUpdate = existingAudio.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingAudio.title),
          questions = questions.getOrElse(existingAudio.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingAudio.thingsToThinkAbout),
          soundcloudId = soundcloudId.getOrElse(existingAudio.soundcloudId)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      }
      yield updatedComponent
    }
  }

  override def updateText(id: UUID, version: Long, ownerId: UUID,
                          title: Option[String],
                          questions: Option[String],
                          thingsToThinkAbout: Option[String],
                          content: Option[String]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        existingText = existingComponent.asInstanceOf[TextComponent]
        componentToUpdate = existingText.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingText.title),
          questions = questions.getOrElse(existingText.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingText.thingsToThinkAbout),
          content = content.getOrElse(existingText.content)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      }
      yield updatedComponent
    }
  }

  override def updateVideo(id: UUID, version: Long, ownerId: UUID,
                           title: Option[String],
                           questions: Option[String],
                           thingsToThinkAbout: Option[String],
                           vimeoId: Option[String],
                           width: Option[Int],
                           height: Option[Int]): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        existingVideo = existingComponent.asInstanceOf[VideoComponent]
        componentToUpdate = existingVideo.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingVideo.title),
          questions = questions.getOrElse(existingVideo.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingVideo.thingsToThinkAbout),
          vimeoId = vimeoId.getOrElse(existingVideo.vimeoId),
          width = width.getOrElse(existingVideo.width),
          height = height.getOrElse(existingVideo.height)
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      }
      yield updatedComponent
    }
  }

  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        component <- lift(componentRepository.find(id))
        toDelete = component match {
          case comp: AudioComponent => comp.copy(version = version)
          case comp: TextComponent => comp.copy(version = version)
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
      val fPart = projectService.findPart(partId)

      for {
        component <- lift(fComponent)
        part <- lift(fPart)
        addedComp <- lift(componentRepository.addToPart(component, part))
      }
      yield component
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
      val fPart = projectService.findPart(partId)

      for {
        component <- lift(fComponent)
        part <- lift(fPart)
        wasRemoved <- lift(componentRepository.removeFromPart(component, part))
      }
      yield component
    }
  }

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
    if (user.roles.map(_.name).contains("administrator")) {
      Future successful \/-(true)
    }
    // Owners can view anything they create
    else if (component.ownerId == user.id) {
      Future successful \/-(true)
    }
    else {
      // Teachers can view the component if it's in one of their projects
      if (user.roles.map(_.name).contains("teacher")) {

        val fAsTeacher: Future[\/[ErrorUnion#Fail, Boolean]] = for {
          courses <- lift(schoolService.listCoursesByTeacher(user.id))
          projects <- liftSeq(courses.map { course => projectService.list(course.id) })
          components <- liftSeq(projects.flatten.map { project => listByProject(project.id) })
        }
        yield components.flatten.contains(component)

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
                courses <- lift(schoolService.listCoursesByTeacher(user.id))
                projects <- liftSeq(courses.map { course => projectService.list(course.id) })
                components <- liftSeq(projects.flatten.map { project => listByProject(project.id) })
              }
              yield components.flatten.contains(component)
              fAsStudent
            }
        }
      }

      // Students can view the component if it's in one of their projects and attached to an active part
      else if (user.roles.map(_.name).contains("student")) {
        for {
          courses <- lift(schoolService.listCoursesByUser(user.id))
          projects <- liftSeq(courses.map { course => projectService.list(course.id) })
          parts = projects.flatten.map(_.parts.filter(_.enabled == true)).flatten
          components <- liftSeq(parts.map { part => listByPart(part.id) })
        }
        yield components.contains(component)
      }
      else {
        Future successful \/-(false)
      }
    }
  }
}