package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait SchoolServiceComponent {

  val schoolService: SchoolService

  trait SchoolService {

    def listCourses: Future[IndexedSeq[Course]]
    def findCourse(id: UUID): Future[Option[Course]]
    def createCourse(name: String): Future[Course]
    def updateCourse(id: UUID, version: Long, name: String): Future[Course]
    def deleteCourse(id: UUID, version: Long): Future[Boolean]

    def listSections: Future[IndexedSeq[Class]]
    def listSectionsByUser(userId: UUID): Future[IndexedSeq[Class]]
    def listSectionsByTeacher(userId: UUID): Future[IndexedSeq[Class]]
    def listSectionsByProject(projectId: UUID): Future[IndexedSeq[Class]]
    def findSection(id: UUID): Future[Option[Class]]
    def createSection(courseId: UUID, teacherId: Option[UUID], name: String): Future[Class]
    def updateSection(id: UUID, version: Long, courseId: UUID, teacherId: Option[UUID], name: String): Future[Class]
    def deleteSection(id: UUID, version: Long): Future[Boolean]

    // Utility functions for section management, assuming you already found a section object.
    def enablePart(classId: UUID, partId: UUID): Future[Boolean]
    def disablePart(classId: UUID, partId: UUID): Future[Boolean]

    def isPartEnabledForUser(partId: UUID, userId: UUID): Future[Boolean]
    def isPartEnabledForSection(partId: UUID, classId: UUID): Future[Boolean]
    def listEnabledParts(projectSlug: String, userId: UUID): Future[IndexedSeq[Part]]
    def listEnabledParts(projectId: UUID, classId: UUID): Future[IndexedSeq[Part]]

    def userHasProject(userId: UUID, projectSlug: String): Future[Boolean]

    //def listStudents(course: Course): Future[IndexedSeq[User]]
    def listStudents(classId: UUID): Future[IndexedSeq[User]]
    def listStudents(section: Class): Future[IndexedSeq[User]]
    def listProjects(classId: UUID): Future[IndexedSeq[Project]]
    def listProjects(section: Class): Future[IndexedSeq[Project]]

    def addUsers(section: Class, userIds: IndexedSeq[UUID]): Future[Boolean]
    def removeUsers(section: Class, userIds: IndexedSeq[UUID]): Future[Boolean]

    def forceComplete(taskId: UUID, classId: UUID): Future[Boolean]
  }
}
