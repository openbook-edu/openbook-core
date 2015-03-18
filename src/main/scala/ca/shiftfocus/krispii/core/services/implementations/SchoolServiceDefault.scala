package ca.shiftfocus.krispii.core.services

import java.awt.Color

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import scala.collection.IndexedSeq
import scala.concurrent.Future
import scalaz.{\/-, -\/, \/}

class SchoolServiceDefault(val db: Connection,
                           val authService: AuthService,
                           val userRepository: UserRepository,
                           val courseRepository: CourseRepository)
  extends SchoolService {

  implicit def conn: Connection = db

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
  override def createCourse(teacherId: UUID, name: String, color: Color): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        teacher <- lift(authService.find(teacherId))
        _ <- predicate (teacher.roles.map(_.name).contains("teacher")) (ServiceError.BadPermissions("Tried to create a course for a user who isn't a teacher."))
        newCourse = Course(
          teacherId = teacher.id,
          name = name,
          color = color
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
}
