package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import java.awt.Color

import scalaz.\/

trait SchoolServiceComponent {

  val schoolService: SchoolService

  trait SchoolService {

    def listCourses: Future[\/[Fail, IndexedSeq[Course]]]
    def listCoursesByUser(userId: UUID): Future[\/[Fail, IndexedSeq[Course]]]
    def listCoursesByTeacher(userId: UUID): Future[\/[Fail, IndexedSeq[Course]]]
    def listCoursesByProject(projectId: UUID): Future[\/[Fail, IndexedSeq[Course]]]

    def findCourse(id: UUID): Future[\/[Fail, Course]]

    def createCourse(teacherId: Option[UUID], name: String, color: Color): Future[\/[Fail, Course]]
    def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: String, color: Color): Future[\/[Fail, Course]]
    def deleteCourse(id: UUID, version: Long): Future[\/[Fail, Course]]

    def userHasProject(userId: UUID, projectSlug: String): Future[\/[Fail, (User, Project)]]

    //def listStudents(course: Course): Future[IndexedSeq[User]]
    def listStudents(courseId: UUID): Future[\/[Fail, IndexedSeq[User]]]
    def listStudents(course: Course): Future[\/[Fail, IndexedSeq[User]]]
    def listProjects(courseId: UUID): Future[\/[Fail, IndexedSeq[Project]]]
    def listProjects(course: Course): Future[\/[Fail, IndexedSeq[Project]]]

    def findUserForTeacher(userId: UUID, teacherId: UUID): Future[Option[UserInfo]]

    def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[Fail, Course]]
    def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[Fail, Course]]

    def forceComplete(taskId: UUID): Future[\/[Fail, Course]]
  }
}
