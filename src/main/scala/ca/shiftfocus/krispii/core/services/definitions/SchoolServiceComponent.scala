package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import java.awt.Color

trait SchoolServiceComponent {

  val schoolService: SchoolService

  trait SchoolService {

    def listCourses: Future[IndexedSeq[Course]]
    def listCoursesByUser(userId: UUID): Future[IndexedSeq[Course]]
    def listCoursesByTeacher(userId: UUID): Future[IndexedSeq[Course]]
    def listCoursesByProject(projectId: UUID): Future[IndexedSeq[Course]]
    def findCourse(id: UUID): Future[Option[Course]]
    def createCourse(teacherId: Option[UUID], name: String, color: Color): Future[Course]
    def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: String, color: Color): Future[Course]
    def deleteCourse(id: UUID, version: Long): Future[Boolean]


    def listEnabledParts(projectSlug: String, userId: UUID): Future[IndexedSeq[Part]]
    // TODO - rewrite
    def listEnabledParts(projectId: UUID): Future[IndexedSeq[Part]]
//    def listEnabledParts(projectId: UUID, classId: UUID): Future[IndexedSeq[Part]]

    def userHasProject(userId: UUID, projectSlug: String): Future[Boolean]

    //def listStudents(course: Course): Future[IndexedSeq[User]]
    def listStudents(courseId: UUID): Future[IndexedSeq[User]]
    def listStudents(course: Course): Future[IndexedSeq[User]]
    def listProjects(courseId: UUID): Future[IndexedSeq[Project]]
    def listProjects(course: Course): Future[IndexedSeq[Project]]

    def findUserForTeacher(userId: UUID, teacherId: UUID): Future[Option[UserInfo]]

    def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[Boolean]
    def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[Boolean]

    def forceComplete(taskId: UUID, courseId: UUID): Future[Boolean]
  }
}
