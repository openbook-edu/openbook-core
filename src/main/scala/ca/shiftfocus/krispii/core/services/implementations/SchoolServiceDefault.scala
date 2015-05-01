package ca.shiftfocus.krispii.core.services

import java.awt.Color

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import scala.collection.IndexedSeq
import scala.concurrent.Future
import scalaz.{\/-, -\/, \/}

class SchoolServiceDefault(val db: DB,
                           val authService: AuthService,
                           val userRepository: UserRepository,
                           val courseRepository: CourseRepository,
                           val chatRepository: ChatRepository)
  extends SchoolService {

  implicit def conn: Connection = db.pool

  /*
   * Methods for Courses
   */

  /**
   * List all courses.
   *
   * @return an [[IndexedSeq]] of [[Course]]
   */
  override def listCourses: Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    courseRepository.list
  }
  
  /**
   * List all courses associated with a specific user.
   *
   * This finds courses for which the given id is set as a
   * student of the course via an association table.
   *
   * @param userId the [[UUID]] of the [[User]] to search for.
   * @return an [[IndexedSeq]] of [[Course]]
   */
  override def listCoursesByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      user <- lift(authService.find(userId))
      courses <- lift(courseRepository.list(user, false))
    }
    yield courses
  }

  /**
   * List all courses taught by a specific user.
   *
   * This finds courses for which the given id is set as the teacherID
   * parameter.
   *
   * @param userId the [[UUID]] of the [[User]] to search for.
   * @return an [[IndexedSeq]] of [[Course]]
   */
  override def listCoursesByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      user <- lift(authService.find(userId))
      courses <- lift(courseRepository.list(user, true))
    } yield courses
  }

  /**
   * Find a specific course by id.
   *
   * @param id the [[UUID]] of the [[Course]] to find.
   * @return an optional [[Course]]
   */
  override def findCourse(id: UUID): Future[\/[ErrorUnion#Fail, Course]] = {
    courseRepository.find(id)
  }

  /**
   * Create a new course.
   *
   * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
   * @param name the name of this course
   * @return the newly created [[Course]]
   */
  override def createCourse(teacherId: UUID, name: String, color: Color, slug: String): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        teacher <- lift(authService.find(teacherId))
        _ <- predicate (teacher.roles.map(_.name).contains("teacher")) (ServiceError.BadPermissions("Tried to create a course for a user who isn't a teacher."))
        newCourse = Course(
          teacherId = teacher.id,
          name = name,
          color = color,
          slug  = slug
        )
        createdCourse <- lift(courseRepository.insert(newCourse))
      }
      yield newCourse
    }
  }

  /**
   * Create a new course.
   *
   * @param id the [[UUID]] of the [[Course]] this course belongs to
   * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
   * @param name the name of this course
   * @return the newly created [[Course]]
   */
  override def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: Option[String], color: Option[Color]): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        tId = teacherId.getOrElse(existingCourse.teacherId)
        teacher <- lift(authService.find(tId))
        _ <- predicate (teacher.roles.map(_.name).contains("teacher")) (ServiceError.BadPermissions("Tried to update a course for a user who isn't a teacher."))
        toUpdate = existingCourse.copy(
          teacherId = teacherId.getOrElse(existingCourse.teacherId),
          name = name.getOrElse(existingCourse.name),
          color = color.getOrElse(existingCourse.color)
        )
        updatedCourse <- lift(courseRepository.update(toUpdate))
      }
      yield updatedCourse
    }
  }

  /**
   * Delete a [[Course]] from the system.
   *
   * @param id the unique ID of the [[Course]] to update
   * @param version the latest version of the [[Course]] for O.O.L.
   * @return a boolean indicating success or failure
   */
  override def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        toDelete = existingCourse.copy(version = version)
        wasDeleted <- lift(courseRepository.delete(toDelete))
      } yield wasDeleted
    }
  }

  // Utility functions for course management, assuming you already found a course object.

  /**
   * List all students registered to a course.
   */
  override def listStudents(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      course <- lift(courseRepository.find(courseId))
      students <- lift(listStudents(course))
    } yield students
  }

  override def listStudents(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    userRepository.list(course)
  }

//  /**
//   * List all projects belonging to a course.
//   */
//  override def listProjects(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
//    for {
//      course <- lift(courseRepository.find(courseId))
//      projects <- lift(listProjects(course))
//    } yield projects
//  }

  /**
   * Add students to a course.
   *
   * @param course the [[Course]] to add users to
   * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be added.
   * @return a [[Boolean]] indicating success or failure.
   */
  override def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    transactional { implicit conn =>
      for {
        users <- lift(userRepository.list(userIds))
        wereAdded <- lift(courseRepository.addUsers(course, users))
        usersInCourse <- lift(userRepository.list(course))
      } yield usersInCourse
    }
  }

  /**
   * Remove students from a course.
   *
   * @param course the [[Course]] to remove users from
   * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be removed.
   * @return [[Boolean]] indicating success or failure.
   */
  override def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    transactional { implicit conn =>
      for {
        users <- lift(userRepository.list(userIds))
        wereRemoved <- lift(courseRepository.removeUsers(course, users))
        usersInCourse <- lift(userRepository.list(course))
      } yield usersInCourse
    }
  }

  /**
   * Given a user and teacher, finds whether this user belongs to any of that teacher's courses.
   *
   * @param userId
   * @param teacherId
   * @return
   */
  override def findUserForTeacher(userId: UUID, teacherId: UUID): Future[\/[ErrorUnion#Fail, User]] = {
    for {
      user        <- lift(authService.find(userId))
      teacher     <- lift(authService.find(teacherId))
      userCourses <- lift(courseRepository.list(user, false))
      filteredCourses = userCourses.filter(_.teacherId == teacherId)
      _ <- predicate (filteredCourses.nonEmpty) (RepositoryError.NoResults)
    } yield user
  }

  /**
   * List all chats for a course.
   *
   * @param courseId
   * @return
   */
  override def listChats(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      chats <- lift(chatRepository.list(course))
    } yield chats
  }

  /**
   * List a slice of chats for a course.
   *
   * @param courseId
   * @param num
   * @param offset
   * @return
   */
  override def listChats(courseId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      _ <- predicate (num > 0 && offset > 0) (ServiceError.BadInput("num, and offset parameters must be positive long integers"))
      chats <- lift(chatRepository.list(course, num, offset))
    } yield chats
  }

  /**
   * List all of one user's chats in a course.
   *
   * @param courseId
   * @param userId
   * @return
   */
  override def listChats(courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      user <- lift(userRepository.find(userId))
      chats <- lift(chatRepository.list(course, user))
    } yield chats
  }

  /**
   * List a slice of one user's chats in a course.
   *
   * @param courseId
   * @param userId
   * @param num
   * @param offset
   * @return
   */
  override def listChats(courseId: UUID, userId: UUID,  num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      user <- lift(userRepository.find(userId))
      _ <- predicate (num > 0 && offset > 0) (ServiceError.BadInput("num, and offset parameters must be positive long integers"))
      chats <- lift(chatRepository.list(course, user, num, offset))
    } yield chats
  }

  /**
   * Find a specific chat message.
   *
   * @param courseId
   * @param messageNum
   * @return
   */
  override def findChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]] = {
    for {
      course <- lift(findCourse(courseId))
      chat <- lift(chatRepository.find(course, messageNum))
    } yield chat
  }

  /**
   * Insert a new chat message.
   *
   * @param courseId
   * @param userId
   * @param message
   * @return
   */
  override def insertChat(courseId: UUID, userId: UUID, message: String): Future[\/[ErrorUnion#Fail, Chat]] = {
    transactional { implicit conn =>
      for {
        course <- lift(findCourse(courseId))
        user <- lift(userRepository.find(userId))
        newChat = Chat(courseId = course.id, userId = userId, message = message)
        createdChat <- lift(chatRepository.insert(newChat))
      } yield createdChat
    }
  }

  /**
   * Update an existing chat message's hidden status.
   *
   * @param courseId
   * @param messageNum
   * @param hidden
   * @return
   */
  override def updateChat(courseId: UUID, messageNum: Long, hidden: Boolean): Future[\/[ErrorUnion#Fail, Chat]] = {
    transactional { implicit conn =>
      for {
        existingChat <- lift(find(courseId, messageNum))
        newChat = existingChat.copy(hidden = hidden)
        updatedChat <- lift(chatRepository.update(newChat))
      } yield updatedChat
    }
  }
}
