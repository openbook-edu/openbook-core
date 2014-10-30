package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait SectionRepositoryComponent {
  val sectionRepository: SectionRepository

  trait SectionRepository {
    def list: Future[IndexedSeq[Section]]
    def list(course: Course): Future[IndexedSeq[Section]]
    def list(project: Project): Future[IndexedSeq[Section]]
    def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Section]]]
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Section]]

    def find(sectionId: UUID): Future[Option[Section]]

    def insert(section: Section)(implicit conn: Connection): Future[Section]
    def update(section: Section)(implicit conn: Connection): Future[Section]
    def delete(section: Section)(implicit conn: Connection): Future[Boolean]

    def addUser(user: User, section: Section)(implicit conn: Connection): Future[Boolean]
    def removeUser(user: User, section: Section)(implicit conn: Connection): Future[Boolean]

    def addUsers(section: Section, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeUsers(section: Section, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeAllUsers(section: Section)(implicit conn: Connection): Future[Boolean]

    def addProjects(section: Section, projects: IndexedSeq[Project])(implicit conn: Connection): Future[Boolean]
    def removeProjects(section: Section, projects: IndexedSeq[Project])(implicit conn: Connection): Future[Boolean]
    def removeAllProjects(section: Section)(implicit conn: Connection): Future[Boolean]

    def enablePart(section: Section, part: Part)(implicit conn: Connection): Future[Boolean]
    def disablePart(section: Section, part: Part)(implicit conn: Connection): Future[Boolean]
    def disablePart(part: Part)(implicit conn: Connection): Future[Boolean]

    def hasProject(user: User, project: Project)(implicit conn: Connection): Future[Boolean]
  }
}
