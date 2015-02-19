package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.fail._
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scalaz.{\/, -\/, \/-}

trait ComponentRepositoryPostgresComponent extends ComponentRepositoryComponent {
  self: PostgresDB =>

  override val componentRepository: ComponentRepository = new ComponentRepositoryPSQL

  private class ComponentRepositoryPSQL extends ComponentRepository {

    val SelectAll = """
      SELECT id, version, owner_id, title, questions, things_to_think_about, type,
             audio_components.component_id as audio_components_id,
             text_components.component_id as text_components_id,
             video_components.component_id as video_components_id,
             soundcloud_id, content, vimeo_id, width, height,
             created_at, updated_at
      FROM components
      LEFT JOIN audio_components ON components.id = audio_components.component_id
      LEFT JOIN text_components ON components.id = text_components.component_id
      LEFT JOIN video_components ON components.id = video_components.component_id
      ORDER BY title ASC
    """

    val SelectOne = """
      SELECT id, version, owner_id, title, questions, things_to_think_about, type,
             audio_components.component_id as audio_components_id,
             text_components.component_id as text_components_id,
             video_components.component_id as video_components_id,
             soundcloud_id, content, vimeo_id, width, height,
             created_at, updated_at
      FROM components
      LEFT JOIN audio_components ON components.id = audio_components.component_id
      LEFT JOIN text_components ON components.id = text_components.component_id
      LEFT JOIN video_components ON components.id = video_components.component_id
        AND components.id = ?
    """

    val SelectByPartId = """
      SELECT id, version, owner_id, title, questions, things_to_think_about, type,
             audio_components.component_id as audio_components_id,
             text_components.component_id as text_components_id,
             video_components.component_id as video_components_id,
             soundcloud_id, content, vimeo_id, width, height,
             created_at, updated_at
      FROM components
      INNER JOIN components_parts ON components.id = components_parts.component_id
      LEFT JOIN audio_components ON components.id = audio_components.component_id
      LEFT JOIN text_components ON components.id = text_components.component_id
      LEFT JOIN video_components ON components.id = video_components.component_id
      WHERE components_parts.part_id = ?
      GROUP BY components.id
      ORDER BY components.title ASC
    """

    val SelectByProjectId = """
      SELECT id, version, owner_id, title, questions, things_to_think_about, type,
             audio_components.component_id as audio_components_id,
             text_components.component_id as text_components_id,
             video_components.component_id as video_components_id,
             soundcloud_id, content, vimeo_id, width, height,
             created_at, updated_at
      FROM components
      INNER JOIN components_parts ON components.id = components_parts.component_id
      INNER JOIN parts
      LEFT JOIN audio_components ON components.id = audio_components.component_id
      LEFT JOIN text_components ON components.id = text_components.component_id
      LEFT JOIN video_components ON components.id = video_components.component_id
      WHERE components_parts.part_id = parts.id
        AND parts.project_id = ?
      GROUP BY components.id
      ORDER BY components.title ASC
    """

    val SelectEnabledByProjectId = """
      SELECT components.id as id, components.version as version, components.owner_id as owner_id,
             components.questions as questions, components.things_to_think_about as things_to_think_about,
             components.type as type, components.title as title,
             audio_components.component_id as audio_components_id,
             text_components.component_id as text_components_id,
             video_components.component_id as video_components_id,
             soundcloud_id, content, vimeo_id, width, height,
             components.created_at as created_at, components.updated_at as updated_at
      FROM components
      INNER JOIN users ON users.id = ?
      INNER JOIN users_classes ON users.id = users_classes.user_id
      INNER JOIN classes_projects ON users_classes.class_id = classes_projects.class_id
      INNER JOIN projects ON projects.id = classes_projects.project_id
      INNER JOIN components_parts ON components.id = components_parts.component_id
      INNER JOIN parts ON projects.id = parts.project_id AND parts.id = components_parts.part_id
      INNER JOIN scheduled_classes_parts ON components_parts.part_id = scheduled_classes_parts.part_id  AND scheduled_classes_parts.part_id = parts.id AND scheduled_classes_parts.class_id = users_classes.class_id
      LEFT JOIN audio_components ON components.id = audio_components.component_id
      LEFT JOIN text_components ON components.id = text_components.component_id
      LEFT JOIN video_components ON components.id = video_components.component_id
      WHERE parts.project_id = ?
      GROUP BY components.id, audio_components.component_id, text_components.component_id, video_components.component_id
      ORDER BY components.title ASC
    """

    val AddToPart = """
      INSERT INTO components_parts (component_id, part_id, created_at) VALUES (?, ?, ?)
    """

    val RemoveFromPart = """
      DELETE FROM components_parts WHERE component_id = ? AND part_id = ?
    """

    val RemoveAllFromParts = """
      DELETE FROM components_parts WHERE part_id = ?
    """

    val InsertAudio = """
      WITH component AS (
        INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
        VALUES (?, 1, ?, ?, ?, ?, 'audio', ?, ?)
        RETURNING id, version
      )
      INSERT INTO audio_components (component_id, soundcloud_id)
        SELECT id, ? as soundcloud_id
        FROM component
    """

    val UpdateAudio = """
      WITH component AS (
        UPDATE components
        SET version = ?, owner_id = ?, title = ?, questions = ?, things_to_think_about = ?, type = 'audio', updated_at = ?
        WHERE id = ?
          AND version = ?
        RETURNING id, version
      )
      UPDATE audio_components
      SET soundcloud_id = ?
      FROM component
      WHERE component_id = component.id
      RETURNING component.version
    """

    val InsertText = """
      WITH component AS (
        INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
        VALUES (?, 1, ?, ?, ?, ?, 'text', ?, ?)
        RETURNING id, version
      )
      INSERT INTO text_components (component_id, content)
        SELECT id, ? as content
        FROM component
    """

    val UpdateText = """
      WITH component AS (
        UPDATE components
        SET version = ?, owner_id = ?, title = ?, questions = ?, things_to_think_about = ?, type = 'text', updated_at = ?
        WHERE id = ?
          AND version = ?
        RETURNING id, version
      )
      UPDATE text_components
      SET content = ?
      FROM component
      WHERE component_id = component.id
      RETURNING component.version
    """

    val InsertVideo = """
      WITH component AS (
        INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
        VALUES (?, 1, ?, ?, ?, ?, 'video', ?, ?)
        RETURNING id, version
      )
      INSERT INTO video_components (component_id, vimeo_id, width, height)
        SELECT id, ? as vimeo_id, ? as width, ? as height
        FROM component
    """

    val UpdateVideo = """
      WITH component AS (
        UPDATE components
        SET version = ?, owner_id = ?, title = ?, questions = ?, things_to_think_about = ?, type = 'video', updated_at = ?
        WHERE id = ?
          AND version = ?
        RETURNING id, version
      )
      UPDATE video_components
      SET vimeo_id = ?, width = ?, height = ?
      FROM component
      WHERE component_id = component.id
      RETURNING component.version
    """

    /**
     * Find all components.
     *
     * @return an array of components.
     */
    override def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]] = {
      conn.sendQuery(SelectAll).map {
        result => buildComponentList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find all components belonging to a specific project part.
     *
     * @param part the part to search in
     * @return an array of components
     */
    override def list(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]] = {
      conn.sendPreparedStatement(SelectByPartId, Array[Any](part.id.bytes)).map {
        result => buildComponentList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find all components enabled for a specific user, in a specific project.
     *
     * @param project the project to search within
     * @return an array of components
     */
    override def list(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]] = {
      conn.sendPreparedStatement(SelectByProjectId, Array[Any](project.id.bytes)).map {
        result => buildComponentList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find all components enabled for a specific user, in a specific project.
     *
     * @param project the project to search within
     * @param user the user to search for
     * @return an array of components
     */
    override def list(project: Project, user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]] = {
      conn.sendPreparedStatement(SelectEnabledByProjectId, Array[Any](user.id.bytes, project.id.bytes)).map {
        result => buildComponentList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional RowData object containing the results
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Component]] = {
      conn.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildComponent(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Add this component to a Part.
     *
     * @param component the component to be added
     * @param part the part to add this component to
     * @return a boolean indicating whether the operation was successful
     */
    override def addToPart(component: Component, part: Part)(implicit conn: Connection):Future[\/[Fail, Component]] = {
      conn.sendPreparedStatement(AddToPart, Array[Any](component.id.bytes, part.id.bytes, new DateTime)).map {
        result => {
          if (result.rowsAffected == 0) {
            -\/(GenericFail("No rows were modified"))
          } else {
            \/-(component)
          }
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Frmove this component from a Part.
     *
     * @param component the component to be removed
     * @param part the part to remove this component from
     * @return a boolean indicating whether the operation was successful
     */
    override def removeFromPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[Fail, Component]] = {
      conn.sendPreparedStatement(RemoveFromPart, Array[Any](component.id.bytes, part.id.bytes, new DateTime)).map {
        result => {
          if (result.rowsAffected == 0) {
            -\/(GenericFail("No rows were modified"))
          } else {
            \/-(component)
          }
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    override def removeFromPart(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Component]]] = {
      val result = for {
        componentsInPart <- lift[IndexedSeq[Component]](list(part))
        deletedComponents <- lift[IndexedSeq[Component]] {
          conn.sendPreparedStatement(RemoveAllFromParts, Array[Any](part.id.bytes)).map {
            result =>
              if (result.rowsAffected == componentsInPart.length) -\/(GenericFail("No rows were modified"))
              else \/-(componentsInPart)
          }
        }
      } yield deletedComponents
      
      result.run.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Insert a new component.
     *
     * @param component the component to be inserted
     * @return the newly created component
     */
    override def insert(component: Component)(implicit conn: Connection): Future[\/[Fail, Component]] = {
      {component match {
        case asAudio: AudioComponent => insertAudio(asAudio)
        case asText: TextComponent => insertText(asText)
        case asVideo: VideoComponent => insertVideo(asVideo)
      }}.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update an existing component
     *
     * @param component the component to be updated
     * @return the updated component
     */
    override def update(component: Component)(implicit conn: Connection): Future[\/[Fail, Component]] = {
      {component match {
        case asAudio: AudioComponent => updateAudio(asAudio)
        case asText: TextComponent => updateText(asText)
        case asVideo: VideoComponent => updateVideo(asVideo)
      }}.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /*
     * --------------------------------------------
     * Private type-specific create/update methods.
     * --------------------------------------------
     */

    /**
     * Insert a new audio component.
     *
     * @param component the audio component to insert
     * @return the created audio component
     */
    private def insertAudio(component: AudioComponent)(implicit conn: Connection): Future[\/[Fail, AudioComponent]] = {
      Logger.debug("[AudioTDG.save] - Performing Insert.")
      conn.sendPreparedStatement(InsertAudio, Array(
        component.id.bytes,
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        new DateTime,
        component.soundcloudId)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[AudioComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update a new audio component.
     *
     * @param component the audio component to update
     * @return the updated audio component
     */
    private def updateAudio(component: AudioComponent)(implicit conn: Connection): Future[\/[Fail, AudioComponent]] = {
      Logger.debug("[AudioTDG.save] - Performing Update.")
      conn.sendPreparedStatement(UpdateAudio, Array(
        (component.version + 1),
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        component.id.bytes,
        component.version,
        component.soundcloudId)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[AudioComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Insert a new text component.
     *
     * @param component the text component to insert
     * @return the created text component
     */
    private def insertText(component: TextComponent)(implicit conn: Connection): Future[\/[Fail, TextComponent]] = {
      Logger.debug("[TextTDG.insert] - Performing Insert.")
      conn.sendPreparedStatement(InsertText, Array(
        component.id.bytes,
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        new DateTime,
        component.content)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[TextComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update a new text component.
     *
     * @param component the text component to update
     * @return the updated text component
     */
    private def updateText(component: TextComponent)(implicit conn: Connection): Future[\/[Fail, TextComponent]] = {
      Logger.debug("[TextTDG.save] - Performing Update.")
      conn.sendPreparedStatement(UpdateText, Array(
        (component.version + 1),
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        component.id.bytes,
        component.version,
        component.content)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[TextComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Insert a new video component.
     *
     * @param component the video component to insert
     * @return the created video component
     */
    private def insertVideo(component: VideoComponent)(implicit conn: Connection): Future[\/[Fail, VideoComponent]] = {
      Logger.debug("[VideoTDG.save] - Performing Insert.")
      conn.sendPreparedStatement(InsertVideo, Array(
        component.id.bytes,
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        new DateTime,
        component.vimeoId,
        component.width,
        component.height)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[VideoComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Update a new video component.
     *
     * @param component the video component to update
     * @return the updated video component
     */
    private def updateVideo(component: VideoComponent)(implicit conn: Connection): Future[\/[Fail, VideoComponent]] = {
      Logger.debug("[VideoTDG.save] - Performing Update.")
      conn.sendPreparedStatement(UpdateVideo, Array(
        component.version + 1,
        component.ownerId.bytes,
        component.title,
        component.questions,
        component.thingsToThinkAbout,
        new DateTime,
        component.id.bytes,
        component.version,
        component.vimeoId,
        component.width,
        component.height)
      ).map {
        result => buildComponent(result.rows).map(_.asInstanceOf[VideoComponent])
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Transform result rows into a single component.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildComponent(maybeResultSet: Option[ResultSet]): \/[Fail, Component] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(Component(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Component from the row returned.", exception))
      }
    }

    /**
     * Converts an optional result set into components list
     *
     * @param maybeResultSet
     * @return
     */
    private def buildComponentList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[Component]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(Component.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Component List from the rows returned.", exception))
      }
    }
  }
}
