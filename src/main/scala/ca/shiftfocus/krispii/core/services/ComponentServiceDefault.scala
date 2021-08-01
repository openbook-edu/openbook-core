package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class ComponentServiceDefault(
  val db: DB,
  val authService: AuthService,
  val projectService: ProjectService,
  val schoolService: SchoolService,
  val componentRepository: ComponentRepository,
  val userRepository: UserRepository
)
    extends ComponentService {

  implicit def conn: Connection = db.pool

  /**
   * List all components.
   *
   * @return an array of components
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    componentRepository.list(db.pool)
  }

  override def listMasterLimit(limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    componentRepository.listMasterLimit(limit, offset)
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
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    audioData: MediaData,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = AudioComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = audioData,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createImage(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    imageData: MediaData,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = ImageComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = imageData,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createGoogle(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    googleData: MediaData,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = GoogleComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = googleData,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createMicrosoft(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    microsoftData: MediaData,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = MicrosoftComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = microsoftData,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createBook(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    fileData: MediaData,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = BookComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = fileData,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createText(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    content: String,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = TextComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      content = content,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createGenericHTML(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    htmlContent: String,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = GenericHTMLComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      htmlContent = htmlContent,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createRubric(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    rubricContent: String,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = RubricComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      rubricContent = rubricContent,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def createVideo(
    id: UUID,
    ownerId: UUID,
    title: String,
    description: String,
    questions: String,
    thingsToThinkAbout: String,
    videoData: MediaData,
    height: Int,
    width: Int,
    order: Int,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None
  ): Future[\/[ErrorUnion#Fail, Component]] = {
    val newComponent = VideoComponent(
      id = id,
      ownerId = ownerId,
      title = title,
      description = description,
      questions = questions,
      thingsToThinkAbout = thingsToThinkAbout,
      mediaData = videoData,
      width = width,
      height = height,
      order = order,
      parentId = parentId,
      parentVersion = parentVersion
    )
    transactional { implicit conn =>
      componentRepository.insert(newComponent)
    }
  }

  override def updateAudio(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    audioData: Option[MediaData],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingAudio.description),
          questions = questions.getOrElse(existingAudio.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingAudio.thingsToThinkAbout),
          mediaData = audioData.getOrElse(existingAudio.mediaData),
          order = order.getOrElse(existingAudio.order),
          isPrivate = isPrivate.getOrElse(existingAudio.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingAudio.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingAudio.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateImage(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    imageData: Option[MediaData],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[ImageComponent])(ServiceError.BadInput("Component type is not image"))
        existingImage = existingComponent.asInstanceOf[ImageComponent]
        componentToUpdate = existingImage.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingImage.title),
          description = description.getOrElse(existingImage.description),
          questions = questions.getOrElse(existingImage.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingImage.thingsToThinkAbout),
          mediaData = imageData.getOrElse(existingImage.mediaData),
          order = order.getOrElse(existingImage.order),
          isPrivate = isPrivate.getOrElse(existingImage.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingImage.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingImage.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateGoogle(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    googleData: Option[MediaData],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[GoogleComponent])(ServiceError.BadInput("Component type is not google"))
        existingGoogle = existingComponent.asInstanceOf[GoogleComponent]
        componentToUpdate = existingGoogle.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingGoogle.title),
          description = description.getOrElse(existingGoogle.description),
          questions = questions.getOrElse(existingGoogle.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingGoogle.thingsToThinkAbout),
          mediaData = googleData.getOrElse(existingGoogle.mediaData),
          order = order.getOrElse(existingGoogle.order),
          isPrivate = isPrivate.getOrElse(existingGoogle.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingGoogle.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingGoogle.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateMicrosoft(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    microsoftData: Option[MediaData],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
    transactional { implicit conn =>
      for {
        existingComponent <- lift(componentRepository.find(id))
        _ <- predicate(existingComponent.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(existingComponent.isInstanceOf[MicrosoftComponent])(ServiceError.BadInput("Component type is not microsoft"))
        existingMicrosoft = existingComponent.asInstanceOf[MicrosoftComponent]
        componentToUpdate = existingMicrosoft.copy(
          version = version,
          ownerId = ownerId,
          title = title.getOrElse(existingMicrosoft.title),
          description = description.getOrElse(existingMicrosoft.description),
          questions = questions.getOrElse(existingMicrosoft.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingMicrosoft.thingsToThinkAbout),
          mediaData = microsoftData.getOrElse(existingMicrosoft.mediaData),
          order = order.getOrElse(existingMicrosoft.order),
          isPrivate = isPrivate.getOrElse(existingMicrosoft.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingMicrosoft.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingMicrosoft.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateBook(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    fileData: Option[MediaData],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingBook.description),
          questions = questions.getOrElse(existingBook.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingBook.thingsToThinkAbout),
          mediaData = fileData.getOrElse(existingBook.mediaData),
          order = order.getOrElse(existingBook.order),
          isPrivate = isPrivate.getOrElse(existingBook.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingBook.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingBook.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateText(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    content: Option[String],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingText.description),
          questions = questions.getOrElse(existingText.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingText.thingsToThinkAbout),
          content = content.getOrElse(existingText.content),
          order = order.getOrElse(existingText.order),
          isPrivate = isPrivate.getOrElse(existingText.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingText.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingText.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateGenericHTML(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    htmlContent: Option[String],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingGenericHTML.description),
          questions = questions.getOrElse(existingGenericHTML.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingGenericHTML.thingsToThinkAbout),
          htmlContent = htmlContent.getOrElse(existingGenericHTML.htmlContent),
          order = order.getOrElse(existingGenericHTML.order),
          isPrivate = isPrivate.getOrElse(existingGenericHTML.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingGenericHTML.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingGenericHTML.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateRubric(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    rubricContent: Option[String],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingRubric.description),
          questions = questions.getOrElse(existingRubric.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingRubric.thingsToThinkAbout),
          rubricContent = rubricContent.getOrElse(existingRubric.rubricContent),
          order = order.getOrElse(existingRubric.order),
          isPrivate = isPrivate.getOrElse(existingRubric.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingRubric.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingRubric.parentVersion
          }
        )
        updatedComponent <- lift(componentRepository.update(componentToUpdate))
      } yield updatedComponent
    }
  }

  override def updateVideo(id: UUID, version: Long, ownerId: UUID,
    title: Option[String],
    description: Option[String],
    questions: Option[String],
    thingsToThinkAbout: Option[String],
    videoData: Option[MediaData],
    width: Option[Int],
    height: Option[Int],
    order: Option[Int],
    isPrivate: Option[Boolean],
    parentId: Option[Option[UUID]] = None,
    parentVersion: Option[Option[Long]] = None): Future[\/[ErrorUnion#Fail, Component]] = {
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
          description = description.getOrElse(existingVideo.description),
          questions = questions.getOrElse(existingVideo.questions),
          thingsToThinkAbout = thingsToThinkAbout.getOrElse(existingVideo.thingsToThinkAbout),
          mediaData = videoData.getOrElse(existingVideo.mediaData),
          width = width.getOrElse(existingVideo.width),
          height = height.getOrElse(existingVideo.height),
          order = order.getOrElse(existingVideo.order),
          isPrivate = isPrivate.getOrElse(existingVideo.isPrivate),
          parentId = parentId match {
            case Some(Some(parentId)) => Some(parentId)
            case Some(None) => None
            case None => existingVideo.parentId
          },
          parentVersion = parentVersion match {
            case Some(Some(parentVersion)) => Some(parentVersion)
            case Some(None) => None
            case None => existingVideo.parentVersion
          }
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
          case comp: ImageComponent => comp.copy(version = version)
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
   * enabled for a group, users in that group will be able to
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
   * enabled for a group, users in that group will be able to
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
