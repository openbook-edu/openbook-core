package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.lib.concurrent.FutureMonad
import ca.shiftfocus.krispii.core.repositories.{ChatRepository, UserRepository, CourseRepository}
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future
import java.awt.Color

import scalaz.\/

trait SchoolService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val userRepository: UserRepository
  val courseRepository: CourseRepository
  val chatRepository: ChatRepository

  def listCourses: Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]
  def listCoursesByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]
  def listCoursesByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]]

  def findCourse(id: UUID): Future[\/[ErrorUnion#Fail, Course]]

  def createCourse(teacherId: UUID, name: String, color: Color, slug: String): Future[\/[ErrorUnion#Fail, Course]]
  def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: Option[String], color: Option[Color]): Future[\/[ErrorUnion#Fail, Course]]
  def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Course]]

  //def listStudents(course: Course): Future[IndexedSeq[User]]
  def listStudents(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def listStudents(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  //def listProjects(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  //def listProjects(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]

  def findUserForTeacher(userId: UUID, teacherId: UUID): Future[\/[ErrorUnion#Fail, User]]

  def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]


  // -- Course chat methods -----

  def listChats(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def list(courseId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]

  def list(courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def list(courseId: UUID, userId: UUID,  num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]

  def find(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]]

  def insert(courseId: UUID, userId: UUID, message: String): Future[\/[ErrorUnion#Fail, Chat]]
  def update(courseId: UUID, messageNum: Long, hidden: Boolean): Future[\/[ErrorUnion#Fail, Chat]]
}