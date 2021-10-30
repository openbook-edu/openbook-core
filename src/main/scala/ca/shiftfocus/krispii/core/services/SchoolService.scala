package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.repositories.{ChatRepository, CourseRepository, UserRepository}
import java.util.UUID

import ca.shiftfocus.krispii.core.models._

import scala.concurrent.Future
import java.awt.Color

import ca.shiftfocus.krispii.core.models.group.Course
import ca.shiftfocus.krispii.core.models.user.User
import org.joda.time.DateTime
import scalaz.\/

import scala.collection.IndexedSeq

trait SchoolService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val userRepository: UserRepository
  val courseRepository: CourseRepository
  val chatRepository: ChatRepository

  def listCourses: Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]
  def listCoursesByUser(userId: UUID, isDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]
  def listCoursesByUser(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]
  def listCoursesByTeacher(userId: UUID, isDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]

  def findCourse(id: UUID): Future[\/[ErrorUnion#Fail, Course]]
  def findCourse(slug: String): Future[\/[ErrorUnion#Fail, Course]]

  def createCourse(teacherId: UUID, name: String, color: Color, slug: String): Future[\/[ErrorUnion#Fail, Course]]
  def updateCourse(
    id: UUID,
    version: Long,
    teacherId: Option[UUID],
    name: Option[String],
    slug: Option[String],
    color: Option[Color],
    enabled: Option[Boolean],
    archived: Option[Boolean],
    schedulingEnabled: Option[Boolean],
    theaterMode: Option[Boolean],
    lastProjectId: Option[Option[UUID]],
    chatEnabled: Option[Boolean]
  ): Future[\/[ErrorUnion#Fail, Course]]
  def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Course]]

  def listStudents(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def listStudents(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]

  def findUserForTeacher(userId: UUID, teacherId: UUID): Future[\/[ErrorUnion#Fail, User]]

  def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def removeUser(course: Course, userId: UUID): Future[\/[ErrorUnion#Fail, User]]

  // -- Course chat methods -----
  def toggleCourseChat(courseId: UUID, chatEnabled: Boolean): Future[\/[ErrorUnion#Fail, Course]]

  def listChats(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(courseId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(courseId: UUID, userId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(course: Course, reader: User, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(courseId: UUID, readerId: UUID, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]

  def findChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]]

  def insertChat(courseId: UUID, userId: UUID, message: String, shouting: Boolean): Future[\/[ErrorUnion#Fail, Chat]]
  def updateChat(courseId: UUID, messageNum: Long, hidden: Boolean): Future[\/[ErrorUnion#Fail, Chat]]
  def deleteChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]]

  def getRandomWord(lang: String): Future[\/[ErrorUnion#Fail, LinkWord]]

  def createLink(lang: String, courseId: UUID): Future[\/[ErrorUnion#Fail, Link]]
  def findLink(link: String): Future[\/[ErrorUnion#Fail, Link]]
  def findLinkByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]]
  def deleteLink(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]]

  // Teacher
  def getCourseLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getStorageUsed(teacherId: UUID): Future[\/[ErrorUnion#Fail, Float]]
  def getStorageLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Float]]
  def getTeacherStudentLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Int]]

  def setCourseLimit(teacherId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setStorageLimit(teacherId: UUID, limit: Float): Future[\/[ErrorUnion#Fail, Float]]
  def setTeacherStudentLimit(teacherId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]

  // Course
  def getCourseStudentLimit(courseId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def setCourseStudentLimit(courseId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def deleteCourseStudentLimit(courseId: UUID): Future[\/[ErrorUnion#Fail, Unit]]

  // Plan
  def getPlanCourseLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]]
  def getPlanStorageLimit(plantId: String): Future[\/[ErrorUnion#Fail, Float]]
  def getPlanStudentLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]]
  def getPlanCopiesLimit(planId: String): Future[\/[ErrorUnion#Fail, Long]]

  def setPlanStorageLimit(planId: String, limitValue: Float): Future[\/[ErrorUnion#Fail, Float]]
  def setPlanCourseLimit(planId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setPlanStudentLimit(planId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setPlanCopiesLimit(planId: String, limitValue: Long): Future[\/[ErrorUnion#Fail, Long]]

  // Organization
  def getOrganizationStudentLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getOrganizationCourseLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getOrganizationStorageLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Float]]
  def getOrganizationDateLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, DateTime]]
  def getOrganizationMemberLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getOrganizationCopiesLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Long]]

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float): Future[\/[ErrorUnion#Fail, Float]]
  def setOrganizationCourseLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setOrganizationStudentLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime): Future[\/[ErrorUnion#Fail, DateTime]]
  def setOrganizationMemberLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setOrganizationCopiesLimit(organizationId: UUID, limit: Long): Future[\/[ErrorUnion#Fail, Long]]
}
