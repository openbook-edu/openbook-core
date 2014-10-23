package com.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.models._
import com.shiftfocus.krispii.core.repositories._
import com.shiftfocus.krispii.core.services.datasource._
import com.shiftfocus.krispii.core.lib.UUID
import play.api.Logger
import scala.concurrent.Future

trait ComponentServiceImplComponent extends ComponentServiceComponent {
  self: ComponentRepositoryComponent with
        UserRepositoryComponent with
        ProjectRepositoryComponent with
        PartRepositoryComponent with
        DB =>

  override val componentService: ComponentService = new ComponentServiceImpl

  private class ComponentServiceImpl extends ComponentService {

    /**
     * List all components.
     *
     * @return an array of components
     */
    override def list: Future[IndexedSeq[Component]] = {
      componentRepository.list(db.pool)
    }

    /**
     * List components by part ID.
     *
     * @param partId the unique ID of the part to filter by
     * @return an array of components
     */
    override def list(partId: UUID): Future[IndexedSeq[Component]] = {
      for {
        part <- partRepository.find(partId).map(_.get)
        componentList <- componentRepository.list(part)(db.pool)
      }
      yield componentList
    }.recover {
      case exception => throw exception
    }

    /**
     * List components by project and user, thus listing "enabled" components.
     *
     * A user can view components that are enabled in any of their sections.
     *
     * @param projectId the unique ID of the part to filter by
     * @param userId the user to check for
     * @return an array of components
     */
    override def list(projectId: UUID, userId: UUID): Future[IndexedSeq[Component]] = {
      val fProject = projectRepository.find(projectId).map(_.get)
      val fUser = userRepository.find(userId).map(_.get)

      for {
        project <- fProject
        user <- fUser
        componentList <- componentRepository.list(project, user)(db.pool)
      }
      yield componentList
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a single component by ID.
     *
     * @param id the unique ID of the component to load.
     * @return an optional component
     */
    override def find(id: UUID): Future[Option[Component]] = {
      componentRepository.find(id)(db.pool)
    }

    override def createAudio(title: String, questions: String, thingsToThinkAbout: String, soundcloudId: String): Future[Component] = {
      val newComponent = AudioComponent(
        title = title,
        questions = questions,
        thingsToThinkAbout = thingsToThinkAbout,
        soundcloudId = soundcloudId
      )
      transactional { implicit connection =>
        componentRepository.insert(newComponent)
      }
    }.recover {
      case exception => throw exception
    }

    override def createText(title: String, questions: String, thingsToThinkAbout: String, content: String): Future[Component] = {
      val newComponent = TextComponent(
        title = title,
        questions = questions,
        thingsToThinkAbout = thingsToThinkAbout,
        content = content
      )
      transactional { implicit connection =>
        componentRepository.insert(newComponent)
      }
    }.recover {
      case exception => throw exception
    }

    override def createVideo(title: String, questions: String, thingsToThinkAbout: String, vimeoId: String, height: Int, width: Int): Future[Component] = {
      val newComponent = VideoComponent(
        title = title,
        questions = questions,
        thingsToThinkAbout = thingsToThinkAbout,
        vimeoId = vimeoId,
        width = width,
        height = height
      )
      transactional { implicit connection =>
        componentRepository.insert(newComponent)
      }
    }.recover {
      case exception => throw exception
    }

    override def updateAudio(id: UUID, version: Long, values: Map[String, Any]): Future[Component] = {
      transactional { implicit connection =>
        for {
          existingComponent <- componentRepository.find(id).map(_.asInstanceOf[AudioComponent])
          componentToUpdate <- Future.successful(existingComponent.copy(
            version = version,
            title = values.get("title") match {
              case Some(title: String) => title
              case _ => existingComponent.title
            },
            questions = values.get("questions") match {
              case Some(questions: String) => questions
              case _ => existingComponent.questions
            },
            thingsToThinkAbout = values.get("thingsToThinkAbout") match {
              case Some(thingsToThinkAbout: String) => thingsToThinkAbout
              case _ => existingComponent.thingsToThinkAbout
            },
            soundcloudId = values.get("soundcloudId") match {
              case Some(soundcloudId: String) => soundcloudId
              case _ => existingComponent.soundcloudId
            }
          ))
          updatedComponent <- componentRepository.update(componentToUpdate)
        }
        yield updatedComponent
      }.recover {
        case exception => throw exception
      }
    }

    override def updateText(id: UUID, version: Long, values: Map[String, Any]): Future[Component] = {
      transactional { implicit connection =>
        for {
          existingComponent <- componentRepository.find(id).map(_.asInstanceOf[TextComponent])
          componentToUpdate <- Future.successful(existingComponent.copy(
            version = version,
            title = values.get("title") match {
              case Some(title: String) => title
              case _ => existingComponent.title
            },
            questions = values.get("questions") match {
              case Some(questions: String) => questions
              case _ => existingComponent.questions
            },
            thingsToThinkAbout = values.get("thingsToThinkAbout") match {
              case Some(thingsToThinkAbout: String) => thingsToThinkAbout
              case _ => existingComponent.thingsToThinkAbout
            },
            content = values.get("content") match {
              case Some(content: String) => content
              case _ => existingComponent.content
            }
          ))
          updatedComponent <- componentRepository.update(componentToUpdate)
        }
        yield updatedComponent
      }.recover {
        case exception => throw exception
      }
    }

    override def updateVideo(id: UUID, version: Long, values: Map[String, Any]): Future[Component] = {
      transactional { implicit connection =>
        for {
          existingComponent <- componentRepository.find(id).map(_.asInstanceOf[VideoComponent])
          componentToUpdate <- Future.successful(existingComponent.copy(
            version = version,
            title = values.get("title") match {
              case Some(title: String) => title
              case _ => existingComponent.title
            },
            questions = values.get("questions") match {
              case Some(questions: String) => questions
              case _ => existingComponent.questions
            },
            thingsToThinkAbout = values.get("thingsToThinkAbout") match {
              case Some(thingsToThinkAbout: String) => thingsToThinkAbout
              case _ => existingComponent.thingsToThinkAbout
            },
            vimeoId = values.get("vimeoId") match {
              case Some(vimeoId: String) => vimeoId
              case _ => existingComponent.vimeoId
            },
            width = values.get("width") match {
              case Some(width: Int) => width.toInt
              case _ => existingComponent.width
            },
            height = values.get("height") match {
              case Some(height: Int) => height.toInt
              case _ => existingComponent.height
            }
          ))
          updatedComponent <- componentRepository.update(componentToUpdate)
        }
        yield updatedComponent
      }.recover {
        case exception => throw exception
      }
    }

    override def delete(id: UUID, version: Long): Future[Boolean] = {
      Future.successful(false)
    }

    /**
     * Add a component to a specific part.
     *
     * This associates a component with a project part. When that part is
     * enabled for a section, users in that section will be able to
     * access that component in their enabled components list.
     *
     * @param componentId the unique ID of the component to add
     * @param partId the unique ID of the part to add the component to
     * @param whether the operation was successful
     */
    override def addToPart(componentId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        val fComponent = componentRepository.find(componentId).map(_.get)
        val fPart = partRepository.find(partId).map(_.get)

        for {
          component <- fComponent
          part <- fPart
          wasAdded <- componentRepository.addToPart(component, part)
        }
        yield wasAdded
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Remove a component from a specific part.
     *
     * This disassociates a component with a project part. When that part is
     * enabled for a section, users in that section will be able to
     * access that component in their enabled components list.
     *
     * @param componentId the unique ID of the component to remove
     * @param partId the unique ID of the part to remove the component from
     * @param whether the operation was successful
     */
    override def removeFromPart(componentId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        val fComponent = componentRepository.find(componentId).map(_.get)
        val fPart = partRepository.find(partId).map(_.get)

        for {
          component <- fComponent
          part <- fPart
          wasRemoved <- componentRepository.removeFromPart(component, part)
        }
        yield wasRemoved
      }.recover {
        case exception => throw exception
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

  }
}
