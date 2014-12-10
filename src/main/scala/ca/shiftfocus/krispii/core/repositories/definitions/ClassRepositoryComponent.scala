package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait ClassRepositoryComponent {
  val classRepository: ClassRepository

  trait ClassRepository {
    def list: Future[IndexedSeq[Class]]
    def list(course: Course): Future[IndexedSeq[Class]]
    def list(project: Project): Future[IndexedSeq[Class]]
    def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Class]]]
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Class]]

    def find(classId: UUID): Future[Option[Class]]

    def insert(section: Class)(implicit conn: Connection): Future[Class]
    def update(section: Class)(implicit conn: Connection): Future[Class]
    def delete(section: Class)(implicit conn: Connection): Future[Boolean]

    def addUser(user: User, section: Class)(implicit conn: Connection): Future[Boolean]
    def removeUser(user: User, section: Class)(implicit conn: Connection): Future[Boolean]

    def addUsers(section: Class, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeUsers(section: Class, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeAllUsers(section: Class)(implicit conn: Connection): Future[Boolean]
    def findUserForTeacher(student: User, teacher: User): Future[Option[User]]

    def addProjects(section: Class, projects: IndexedSeq[Project])(implicit conn: Connection): Future[Boolean]
    def removeProjects(section: Class, projects: IndexedSeq[Project])(implicit conn: Connection): Future[Boolean]
    def removeAllProjects(section: Class)(implicit conn: Connection): Future[Boolean]

    def enablePart(section: Class, part: Part)(implicit conn: Connection): Future[Boolean]
    def disablePart(section: Class, part: Part)(implicit conn: Connection): Future[Boolean]
    def disablePart(part: Part)(implicit conn: Connection): Future[Boolean]

    def hasProject(user: User, project: Project)(implicit conn: Connection): Future[Boolean]
  }
}
