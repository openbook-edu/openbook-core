package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import java.awt.Color

import scalaz.\/

trait SchoolServiceComponent {

  val schoolService: SchoolService

  trait SchoolService {

    def listCourses: Future[\/[ServiceError, IndexedSeq[Course]]]
    def listCoursesByUser(userId: UUID): Future[\/[ServiceError, IndexedSeq[Course]]]
    def listCoursesByTeacher(userId: UUID): Future[\/[ServiceError, IndexedSeq[Course]]]
    def listCoursesByProject(projectId: UUID): Future[\/[ServiceError, IndexedSeq[Course]]]

    def findCourse(id: UUID): Future[\/[ServiceError, Course]]

    def createCourse(teacherId: Option[UUID], name: String, color: Color): Future[\/[ServiceError, Course]]
    def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: String, color: Color): Future[\/[ServiceError, Course]]
    def deleteCourse(id: UUID, version: Long): Future[\/[ServiceError, Course]]

    def userHasProject(userId: UUID, projectSlug: String): Future[\/[ServiceError, (User, Project)]]

    //def listStudents(course: Course): Future[IndexedSeq[User]]
    def listStudents(courseId: UUID): Future[\/[ServiceError, IndexedSeq[User]]]
    def listStudents(course: Course): Future[\/[ServiceError, IndexedSeq[User]]]
    def listProjects(courseId: UUID): Future[\/[ServiceError, IndexedSeq[Project]]]
    def listProjects(course: Course): Future[\/[ServiceError, IndexedSeq[Project]]]

    def findUserForTeacher(userId: UUID, teacherId: UUID): Future[Option[UserInfo]]

    def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ServiceError, Course]]
    def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ServiceError, Course]]

    def forceComplete(taskId: UUID): Future[\/[ServiceError, Course]]
  }
}
