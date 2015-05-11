package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import scalacache.ScalaCache
import scalaz.{\/, -\/, \/-}


class ComponentRepositoryPostgres()
  extends ComponentRepository with PostgresRepository[Component] {

  override def constructor(row: RowData): Component = {
    row("type").asInstanceOf[String] match {
      case "audio" => constructAudio(row)
      case "text" => constructText(row)
      case "video" => constructVideo(row)
    }
  }

  private def constructAudio(row: RowData): AudioComponent = {
    AudioComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("owner_id").asInstanceOf[Array[Byte]]),
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("soundcloud_id").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructText(row: RowData): TextComponent = {
    TextComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("owner_id").asInstanceOf[Array[Byte]]),
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("content").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructVideo(row: RowData): VideoComponent = {
    VideoComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("owner_id").asInstanceOf[Array[Byte]]),
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("vimeo_id").asInstanceOf[String],
      row("width").asInstanceOf[Int],
      row("height").asInstanceOf[Int],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table                 = "components"
  val CommonFields          = "id, version, owner_id, title, questions, things_to_think_about, created_at, updated_at, type"
  def CommonFieldsWithTable(table: String = Table): String = {
    CommonFields.split(", ").map({ field => s"${table}." + field}).mkString(", ")
  }
  val SpecificFields =
    s"""
       |  text_components.content,
       |  video_components.vimeo_id,
       |  video_components.width,
       |  video_components.height,
       |  audio_components.soundcloud_id
     """.stripMargin

  val QMarks  = "?, ?, ?, ?, ?, ?, ?, ?, ?"
  val GroupBy = s"${Table}.id"
  val OrderBy = s"${Table}.title ASC"
  val Join =
    s"""
      |LEFT JOIN  text_components ON $Table.id = text_components.component_id
      |LEFT JOIN  video_components ON $Table.id = video_components.component_id
      |LEFT JOIN  audio_components ON $Table.id = audio_components.component_id
     """.stripMargin

  // -- Select queries -----------------------------------------------------------------------------------------------

  val SelectAll =
    s"""
      |SELECT $CommonFields, $SpecificFields
      |FROM $Table
      |$Join
      |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne =
    s"""
      |SELECT $CommonFields, $SpecificFields
      |FROM $Table
      |$Join
      |WHERE components.id = ?
  """.stripMargin

  val SelectByPartId =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |INNER JOIN parts_components
      |  ON $Table.id = parts_components.component_id
      |WHERE parts_components.part_id = ?
      |ORDER BY $OrderBy
  """.stripMargin

  val SelectByProjectId =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |INNER JOIN parts_components
      | ON components.id = parts_components.component_id
      |INNER JOIN parts
      | ON parts_components.part_id = parts.id
      | AND parts.project_id = ?
      |GROUP BY $GroupBy, $SpecificFields
      |ORDER BY $OrderBy
  """.stripMargin

  val SelectEnabledByProjectId =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |INNER JOIN users
      |  ON users.id = ?
      |INNER JOIN users_courses
      | ON users_courses.user_id = users.id
      |INNER JOIN courses
      | ON courses.id = users_courses.course_id
      |INNER JOIN projects
      | ON projects.course_id = courses.id
      |INNER JOIN parts_components
      | ON parts_components.component_id = components.id
      |INNER JOIN parts
      | ON parts.project_id = projects.id
      | AND parts.id = parts_components.part_id
      |WHERE parts.project_id = ?
      |  AND parts.enabled = 't'
      |GROUP BY $GroupBy, $SpecificFields
      |ORDER BY $OrderBy
  """.stripMargin

  // -- Insert queries -----------------------------------------------------------------------------------------------

  val AddToPart =
    """
    |INSERT INTO parts_components (component_id, part_id, created_at)
    |VALUES (?, ?, ?)
  """.stripMargin

  val Insert =
    s"""
      |INSERT INTO $Table ($CommonFields)
      |VALUES ($QMarks)
      |RETURNING $CommonFields
    """.stripMargin

  val InsertAudio =
    s"""
      |WITH c AS ($Insert),
      |     a AS (INSERT INTO audio_components (component_id, soundcloud_id)
      |           SELECT id as component_id, ? as soundcloud_id
      |           FROM c
      |           RETURNING soundcloud_id)
      |SELECT ${CommonFieldsWithTable("c")}, a.soundcloud_id
      |FROM c, a
  """.stripMargin

  val InsertText =
    s"""
      |WITH c AS ($Insert),
      |     t AS (INSERT INTO text_components (component_id, content)
      |           SELECT id as component_id, ? as content
      |           FROM c
      |           RETURNING content)
      |SELECT ${CommonFieldsWithTable("c")}, t.content
      |FROM c, t
  """.stripMargin

  val InsertVideo =
    s"""
      |WITH c AS ($Insert),
      |     v AS (INSERT INTO video_components (component_id, vimeo_id, width, height)
      |           SELECT id as component_id, ? as vimeo_id, ? as width, ? as height
      |           FROM c
      |           RETURNING vimeo_id, width, height)
      |SELECT ${CommonFieldsWithTable("c")}, v.vimeo_id, v.width, v.height
      |FROM c, v
  """.stripMargin

  // -- Update queries -----------------------------------------------------------------------------------------------

  val Update =
    s"""
      |UPDATE $Table
      |SET version = ?, owner_id = ?,
      |    title = ?, questions = ?,
      |    things_to_think_about = ?,
      |    updated_at = ?
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $CommonFields
     """.stripMargin

  val UpdateText =
    s"""
      |WITH component AS ($Update)
      |UPDATE text_components as t
      |SET content = ?
      |FROM component
      |WHERE component_id = component.id
      |RETURNING $CommonFields,
      |          t.content
    """.stripMargin

  val UpdateVideo =
    s"""
      |WITH component AS ($Update)
      |UPDATE video_components as v
      |SET vimeo_id = ?, width = ?, height = ?
      |FROM component
      |WHERE component_id = component.id
      |RETURNING $CommonFields,
      |          v.vimeo_id, v.width, v.height
    """.stripMargin

  val UpdateAudio =
    s"""
      |WITH component AS ($Update)
      |UPDATE audio_components as a
      |SET soundcloud_id = ?
      |FROM component
      |WHERE component_id = component.id
      |RETURNING $CommonFields,
      |          a.soundcloud_id
    """.stripMargin

  // -- Delete queries -----------------------------------------------------------------------------------------------

  val RemoveFromPart =
    """
     |DELETE FROM parts_components
     |WHERE component_id = ?
     |  AND part_id = ?
   """.stripMargin

  val RemoveAllFromParts =
    """
     |DELETE FROM parts_components
     |WHERE part_id = ?
   """.stripMargin

  val Delete =
    s"""
      |DELETE FROM $Table
      |USING
      | audio_components,
      | text_components,
      | video_components
      |WHERE $Table.id = ?
      | AND $Table.version = ?
      |RETURNING $CommonFields, $SpecificFields
    """.stripMargin

  // -- Methods ------------------------------------------------------------------------------------------------------

  /**
   * Find all components.
   *
   * @return an array of components.
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    queryList(SelectAll)
  }

  /**
   * Find all components belonging to a specific project part.
   *
   * @param part the part to search in
   * @return an array of components
   */
  override def list(part: Part)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    getCached[IndexedSeq[Component]](cacheComponentsKey(part.id)).flatMap {
      case \/-(compList) => Future successful \/-(compList)
      case -\/(RepositoryError.NoResults) =>
        for {
          compList <- lift(queryList(SelectByPartId, Array[Any](part.id.bytes)))
          _ <- lift(putCache[IndexedSeq[Component]](cacheComponentsKey(part.id))(compList, ttl))
        } yield compList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all components in a specific project.
   *
   * @param project the project to search within
   * @return an array of components
   */
  override def list(project: Project)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    getCached[IndexedSeq[Component]](cacheComponentsKey(project.id)).flatMap {
      case \/-(compList) => Future successful \/-(compList)
      case -\/(RepositoryError.NoResults) =>
        for {
          compList <- lift(queryList(SelectByProjectId, Array[Any](project.id.bytes)))
          _ <- lift(putCache[IndexedSeq[Component]](cacheComponentsKey(project.id))(compList, ttl))
        } yield compList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all components enabled for a specific user, in a specific project.
   *
   * @param project the project to search within
   * @param user the user to search for
   * @return an array of components
   */
  override def list(project: Project, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    queryList(SelectEnabledByProjectId, Array[Any](user.id.bytes, project.id.bytes))
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Component]] = {
    getCached[Component](cacheComponentKey(id)).flatMap {
      case \/-(compList) => Future successful \/-(compList)
      case -\/(RepositoryError.NoResults) =>
        for {
          compList <- lift(queryOne(SelectOne, Array[Any](id.bytes)))
          _ <- lift(putCache[Component](cacheComponentKey(id))(compList, ttl))
        } yield compList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Add this component to a Part.
   *
   * @param component the component to be added
   * @param part the part to add this component to
   * @return a boolean indicating whether the operation was successful
   */
  override def addToPart(component: Component, part: Part)(implicit conn: Connection, cache: ScalaCache):Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(AddToPart, Array[Any](component.id.bytes, part.id.bytes, new DateTime))(1 == _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(RepositoryError.NoResults)
        case -\/(error) => -\/(error)
      })
      _ <- lift(removeCached(cacheComponentsKey(part.id)))
    } yield ()
  }

  /**
   * Remove this component from a Part.
   *
   * @param component the component to be removed
   * @param part the part to remove this component from
   * @return a boolean indicating whether the operation was successful
   */
  override def removeFromPart(component: Component, part: Part)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(RemoveFromPart, Array[Any](component.id.bytes, part.id.bytes))(1 == _).map {
        case \/-(true) => \/-( () )
        case \/-(false) => -\/(RepositoryError.NoResults)
        case -\/(error) => -\/(error)
      })
      _ <- lift(removeCached(cacheComponentsKey(part.id)))
    } yield ()
  }

  /**
   * Remove all components from a part.
   *
   * @param part
   * @param conn
   * @return
   */
  override def removeFromPart(part: Part)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    (for {
      componentsInPart <- lift(list(part))
      deletedComponents <- lift {
        queryNumRows(RemoveAllFromParts, Array[Any](part.id.bytes))(componentsInPart.length == _).map {
          case \/-(true) => \/-( componentsInPart )
          case \/-(false) => -\/(RepositoryError.NoResults)
          case -\/(error) => -\/(error)
        }
      }
      _ <- lift(removeCached(cacheComponentsKey(part.id)))
    } yield deletedComponents).run
  }

  /**
   * Insert a new component.
   *
   * @param component the component to be inserted
   * @return the newly created component
   */
  override def insert(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Component]] = {
    // Common properties.
    val commonData = Seq[Any](
      component.id.bytes,
      1,
      component.ownerId.bytes,
      component.title,
      component.questions,
      component.thingsToThinkAbout,
      new DateTime,
      new DateTime
    )

    // Specific properties
    val dataArray = component match {
      case textComponent: TextComponent => commonData ++ Array[Any](
        Component.Text,
        textComponent.content
      )
      case videoComponent: VideoComponent => commonData ++ Array[Any](
        Component.Video,
        videoComponent.vimeoId,
        videoComponent.width,
        videoComponent.height
      )
      case audioComponent: AudioComponent => commonData ++ Array[Any](
        Component.Audio,
        audioComponent.soundcloudId
      )
      case _ => throw new Exception("I don't know how you did this, but you sent me a component type that doesn't exist.")
    }

    val query = component match {
      case textComponent: TextComponent   => InsertText
      case videoComponent: VideoComponent => InsertVideo
      case audioComponent: AudioComponent => InsertAudio
    }

    // Send the query
    queryOne(query, dataArray)
  }

  /**
   * Update an existing component
   *
   * @param component the component to be updated
   * @return the updated component
   */
  override def update(component: Component)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Component]] = {
    // Common properties.
    val commonData = Seq[Any](
      component.version + 1,
      component.ownerId.bytes,
      component.title,
      component.questions,
      component.thingsToThinkAbout,
      new DateTime,
      component.id.bytes,
      component.version
    )

    // Specific properties
    val dataArray = component match {
      case textComponent: TextComponent => commonData ++ Array[Any](
        textComponent.content
      )
      case videoComponent: VideoComponent => commonData ++ Array[Any](
        videoComponent.vimeoId,
        videoComponent.width,
        videoComponent.height
      )
      case audioComponent: AudioComponent => commonData ++ Array[Any](
        audioComponent.soundcloudId
      )
      case _ => throw new Exception("I don't know how you did this, but you sent me a component type that doesn't exist.")
    }

    val query = component match {
      case textComponent: TextComponent   => UpdateText
      case videoComponent: VideoComponent => UpdateVideo
      case audioComponent: AudioComponent => UpdateAudio
    }

    // Send the query
    for {
      updated <- lift(queryOne(query, dataArray))
      _ <- lift(removeCached(cacheComponentKey(component.id)))
    } yield updated
  }

  /**
   * Delete a component.
   *
   * @param component
   * @param conn
   * @return
   */
  override def delete(component: Component)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Component]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](component.id.bytes, component.version)))
      _ <- lift(removeCached(cacheComponentKey(component.id)))
    } yield deleted
  }
}
