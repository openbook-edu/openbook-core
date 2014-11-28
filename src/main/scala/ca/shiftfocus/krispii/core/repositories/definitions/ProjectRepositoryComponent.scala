package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait ProjectRepositoryComponent {
  /**
   * Value storing an instance of the repository. Should be overridden with
   * a concrete implementation to be used via dependency injection.
   */
  val projectRepository: ProjectRepository

  trait ProjectRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list: Future[IndexedSeq[Project]]
    def list(section: Section): Future[IndexedSeq[Project]]
    def find(id: UUID): Future[Option[Project]]
    def find(slug: String): Future[Option[Project]]
    def insert(project: Project)(implicit conn: Connection): Future[Project]
    def update(project: Project)(implicit conn: Connection): Future[Project]
    def delete(project: Project)(implicit conn: Connection): Future[Boolean]

    /**
     * Project *--* Section relationship methods.
     *
     * These methods manipulate the table associating projects to sections.
     */
    def addToSection(section: Section, project: Project)(implicit conn: Connection): Future[Boolean]
    def removeFromSection(section: Section, project: Project)(implicit conn: Connection): Future[Boolean]
    def removeFromAllSections(project: Project)(implicit conn: Connection): Future[Boolean]

  }
}
