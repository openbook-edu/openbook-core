package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait ProjectRepositoryComponent {
  val projectRepository: ProjectRepository

  trait ProjectRepository {
    def list: Future[\/[RepositoryError, IndexedSeq[Project]]]
    def list(course: Course): Future[\/[RepositoryError, IndexedSeq[Project]]]

    def find(id: UUID): Future[\/[RepositoryError, Project]]
    def find(projectId: UUID, user: User): Future[\/[RepositoryError, Project]]
    def find(slug: String): Future[\/[RepositoryError, Project]]

    def insert(project: Project)(implicit conn: Connection): Future[\/[RepositoryError, Project]]
    def update(project: Project)(implicit conn: Connection): Future[\/[RepositoryError, Project]]
    def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError, Project]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Project] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Project]] _
  }
}
