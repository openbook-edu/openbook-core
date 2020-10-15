package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.repositories.{ChatRepository, CourseRepository, UserRepository}
import java.util.UUID

import ca.shiftfocus.krispii.core.models._

import scala.concurrent.Future
import java.awt.Color

import ca.shiftfocus.krispii.core.models.course.Course
import org.joda.time.DateTime
import scalaz.\/

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

  //def listStudents(course: Course): Future[IndexedSeq[User]]
  def listStudents(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def listStudents(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  //def listProjects(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  //def listProjects(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]

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

  def findChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]]

  def insertChat(courseId: UUID, userId: UUID, message: String): Future[\/[ErrorUnion#Fail, Chat]]
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
  def getPlanStudentLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]]
  def getPlanCourseLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]]
  def getPlanStorageLimit(plantId: String): Future[\/[ErrorUnion#Fail, Float]]

  def setPlanStorageLimit(palnId: String, limitValue: Float): Future[\/[ErrorUnion#Fail, Float]]
  def setPlanCourseLimit(palnId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setPlanStudentLimit(palnId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]]

  // Organization
  def getOrganizationStudentLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getOrganizationCourseLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]]
  def getOrganizationStorageLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, Float]]
  def getOrganizationDateLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, DateTime]]
  def getOrganizationMemberLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, Int]]

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float): Future[\/[ErrorUnion#Fail, Float]]
  def setOrganizationCourseLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setOrganizationStudentLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime): Future[\/[ErrorUnion#Fail, DateTime]]
  def setOrganizationMemberLimit(organizationtId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]]
}
