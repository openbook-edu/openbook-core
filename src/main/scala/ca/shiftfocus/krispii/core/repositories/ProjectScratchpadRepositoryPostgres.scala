package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.{ Project, _ }
import com.github.mauricio.async.db.{ Connection, RowData }
import play.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/

class ProjectScratchpadRepositoryPostgres(val documentRepository: DocumentRepository)
    extends ProjectScratchpadRepository with PostgresRepository[ProjectScratchpad] {

  override val entityName = "ProjectScratchpad"
  val Table = "project_notes"
  val Fields = "user_id, project_id, document_id"
  val QMarks = "?, ?, ?"
  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin
  }
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin
  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
       |  AND project_id = ?
       |LIMIT 1
     """.stripMargin
  val SelectAllForUser =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
     """.stripMargin
  val SelectAllForProject =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE project_id = ?
     """.stripMargin
  val DeleteOne =
    s"""
       |DELETE FROM $Table
       |WHERE user_id = ?
       |  AND project_id = ?
       |RETURNING $Fields
     """.stripMargin
  val DeleteAllForProject =
    s"""
       |DELETE FROM $Table
       |WHERE project_id = ?
       |RETURNING $Fields
     """.stripMargin

  override def constructor(row: RowData): ProjectScratchpad = {
    ProjectScratchpad(
      userId = row("user_id").asInstanceOf[UUID],
      projectId = row("project_id").asInstanceOf[UUID],
      documentId = row("document_id").asInstanceOf[UUID]
    )
  }

  /**
   * List a user's latest revisions for all project scratchpads for all projects.
   *
   * @param user the user whose scratchpad it is
   * @return an array of ProjectScratchpad objects representing each scratchpad
   */
  override def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]] = {
    (for {
      projectScratchpadList <- lift(queryList(SelectAllForUser, Seq[Any](user.id)))
      result <- liftSeq(projectScratchpadList.map(projectScratchpad =>
        (for {
          document <- lift(documentRepository.find(projectScratchpad.documentId))
          result = projectScratchpad.copy(
            version = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run))
    } yield result).run
  }

  /**
   * Find the latest revision of a project scratchpad.
   *
   * @param user the user whose scratchpad it is
   * @param project the project this scratchpad is for
   * @return an optional ProjectScratchpad object
   */
  override def find(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]] = {
    (for {
      projectScratchpad <- lift(queryOne(SelectOne, Seq[Any](user.id, project.id)))
      document <- lift(documentRepository.find(projectScratchpad.documentId))
    } yield projectScratchpad.copy(
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt,
      document = Some(document)
    )).run
  }

  /**
   * Insert a new ProjectScratchpad. Used to create new scratchpads, and to insert new
   * revisions to existing pads.
   * @param scratchpad the ProjectScratchpad object to be inserted.
   * @return the newly created ProjectScratchpad
   */
  override def insert(scratchpad: ProjectScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]] = {
    Logger.debug(scratchpad.toString)
    (for {
      projectScratchpad <- lift(queryOne(Insert, Array[Any](
        scratchpad.userId,
        scratchpad.projectId,
        scratchpad.documentId
      )))
      document <- lift(documentRepository.find(projectScratchpad.documentId))
    } yield projectScratchpad.copy(
      document = Some(document),
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt
    )).run
  }

  /**
   * Deletes a project scratchpad.
   *
   * @param projectScratchpad
   * @return
   */
  override def delete(projectScratchpad: ProjectScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]] = {
    (for {
      projectScratchpad <- lift(queryOne(DeleteOne, Seq(
        projectScratchpad.userId,
        projectScratchpad.projectId
      )))
      document <- lift(documentRepository.find(projectScratchpad.documentId))
    } yield projectScratchpad.copy(
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt
    )).run
  }
  /**
   * Deletes all revisions of a project response for a particular project.
   *
   * @param project the project to delete the response for
   * @return
   */
  override def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]] = {
    (for {
      currentList <- lift(list(project))
      _ <- lift(queryList(DeleteAllForProject, Seq[Any](project.id)))
    } yield currentList).run
  }

  /**
   * List all users latest scratchpad revisions to a particular project.
   *
   * @param project the project to list scratchpads for
   * @return an array of ProjectScratchpad objects representing each scratchpad
   */
  override def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]] = {
    (for {
      projectScratchpadList <- lift(queryList(SelectAllForProject, Seq[Any](project.id)))
      result <- liftSeq(projectScratchpadList.map(projectScratchpad =>
        (for {
          document <- lift(documentRepository.find(projectScratchpad.documentId))
          result = projectScratchpad.copy(
            version = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run))
    } yield result).run
  }
}
