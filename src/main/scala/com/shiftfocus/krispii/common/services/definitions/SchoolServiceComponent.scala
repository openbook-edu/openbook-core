package com.shiftfocus.krispii.common.services

import com.shiftfocus.krispii.lib.UUID
import com.shiftfocus.krispii.common.models._
import scala.concurrent.Future

trait SchoolServiceComponent {

  val schoolService: SchoolService

  trait SchoolService {

    def listCourses: Future[IndexedSeq[Course]]
    def findCourse(id: UUID): Future[Option[Course]]
    def createCourse(name: String): Future[Course]
    def updateCourse(id: UUID, version: Long, name: String): Future[Course]
    def deleteCourse(id: UUID, version: Long): Future[Boolean]

    def listSections: Future[IndexedSeq[Section]]
    def listSectionsByUser(userId: UUID): Future[IndexedSeq[Section]]
    def listSectionsByTeacher(userId: UUID): Future[IndexedSeq[Section]]
    def listSectionsByProject(projectId: UUID): Future[IndexedSeq[Section]]
    def findSection(id: UUID): Future[Option[Section]]
    def createSection(courseId: UUID, teacherId: Option[UUID], name: String): Future[Section]
    def updateSection(id: UUID, version: Long, courseId: UUID, teacherId: Option[UUID], name: String): Future[Section]
    def deleteSection(id: UUID, version: Long): Future[Boolean]

    // Utility functions for section management, assuming you already found a section object.
    def enablePart(sectionId: UUID, partId: UUID): Future[Boolean]
    def disablePart(sectionId: UUID, partId: UUID): Future[Boolean]

    def isPartEnabledForUser(partId: UUID, userId: UUID): Future[Boolean]
    def isPartEnabledForSection(partId: UUID, sectionId: UUID): Future[Boolean]
    def listEnabledParts(projectSlug: String, userId: UUID): Future[IndexedSeq[Part]]
    def listEnabledParts(projectId: UUID, sectionId: UUID): Future[IndexedSeq[Part]]

    def userHasProject(userId: UUID, projectSlug: String): Future[Boolean]

    //def listStudents(course: Course): Future[IndexedSeq[User]]
    def listStudents(sectionId: UUID): Future[IndexedSeq[User]]
    def listStudents(section: Section): Future[IndexedSeq[User]]
    def listProjects(sectionId: UUID): Future[IndexedSeq[Project]]
    def listProjects(section: Section): Future[IndexedSeq[Project]]

    def addProjects(section: Section, projectIds: IndexedSeq[UUID]): Future[Boolean]
    def removeProjects(section: Section, projectIds: IndexedSeq[UUID]): Future[Boolean]
    def removeAllProjects(section: Section): Future[Boolean]

    def addUsers(section: Section, userIds: IndexedSeq[UUID]): Future[Boolean]
    def removeUsers(section: Section, userIds: IndexedSeq[UUID]): Future[Boolean]

    def forceComplete(taskId: UUID, sectionId: UUID): Future[Boolean]
  }
}
