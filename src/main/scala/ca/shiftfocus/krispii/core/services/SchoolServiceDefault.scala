package ca.shiftfocus.krispii.core.services

import java.awt.Color

import org.joda.time.DateTime
import play.api.Logger

// scalastyle:ignore
import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import java.util.UUID
import scala.collection.IndexedSeq
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ \/-, -\/, \/ }

class SchoolServiceDefault(
  val db: DB,
  val scalaCache: ScalaCachePool,
  val authService: AuthService,
  val userRepository: UserRepository,
  val courseRepository: CourseRepository,
  val chatRepository: ChatRepository,
  val wordRepository: WordRepository,
  val linkRepository: LinkRepository,
  val limitRepository: LimitRepository
)
    extends SchoolService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /*
   * Methods for Courses
   */

  /**
   * List all courses.
   *
   * @return an IndexedSeq of course
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
   * @param userId the UUID of the user to search for.
   * @return an IndexedSeq of course
   */
  override def listCoursesByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      user <- lift(authService.find(userId))
      courses <- lift(listCoursesByUser(user))
    } yield courses
  }

  override def listCoursesByUser(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    courseRepository.list(user, false)
  }

  /**
   * List all courses taught by a specific user.
   *
   * This finds courses for which the given id is set as the teacherID
   * parameter.
   *
   * @param userId the UUID of the user to search for.
   * @return an IndexedSeq of course
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
   * @param id the UUID of the course to find.
   * @return an optional course
   */
  override def findCourse(id: UUID): Future[\/[ErrorUnion#Fail, Course]] = {
    courseRepository.find(id)
  }

  /**
   * Find a specific course by slug.
   *
   * @param slug the string of the course to find.
   * @return an optional course
   */
  override def findCourse(slug: String): Future[\/[ErrorUnion#Fail, Course]] = {
    courseRepository.find(slug)
  }

  // TODO validate slug
  /**
   * Create a new course.
   *
   * @param teacherId the optional UUID of the user teaching this course
   * @param name the name of this course
   * @return the newly created course
   */
  override def createCourse(teacherId: UUID, name: String, color: Color, slug: String): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        teacher <- lift(authService.find(teacherId))
        _ <- predicate(teacher.roles.map(_.name).contains("teacher"))(ServiceError.BadPermissions("Tried to create a course for a user who isn't a teacher."))
        _ <- lift(isValidSlug(slug))
        newCourse = Course(
          teacherId = teacher.id,
          name = name,
          color = color,
          slug = slug
        )
        createdCourse <- lift(courseRepository.insert(newCourse))
      } yield createdCourse
    }
  }

  /**
   * Create a new course.
   *
   * @param id the UUID of the course this course belongs to
   * @param teacherId the optional UUID of the user teaching this course
   * @param name the name of this course
   * @return the newly created course
   */
  override def updateCourse(
    id: UUID,
    version: Long,
    teacherId: Option[UUID],
    name: Option[String],
    slug: Option[String],
    color: Option[Color],
    enabled: Option[Boolean],
    schedulingEnabled: Option[Boolean],
    chatEnabled: Option[Boolean]
  ): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        tId = teacherId.getOrElse(existingCourse.teacherId)
        teacher <- lift(authService.find(tId))
        _ <- predicate(slug.isEmpty || !existingCourse.enabled)(ServiceError.BusinessLogicFail("You can only change the slug for disabled courses."))
        _ <- predicate(teacher.roles.map(_.name).contains("teacher"))(ServiceError.BadPermissions("Tried to update a course for a user who isn't a teacher."))
        toUpdate = existingCourse.copy(
          teacherId = teacherId.getOrElse(existingCourse.teacherId),
          name = name.getOrElse(existingCourse.name),
          slug = slug.getOrElse(existingCourse.slug),
          color = color.getOrElse(existingCourse.color),
          enabled = enabled.getOrElse(existingCourse.enabled),
          schedulingEnabled = schedulingEnabled.getOrElse(existingCourse.schedulingEnabled),
          chatEnabled = chatEnabled.getOrElse(existingCourse.chatEnabled)
        )
        updatedCourse <- lift(courseRepository.update(toUpdate))
      } yield updatedCourse
    }
  }

  /**
   * Delete a course from the system.
   *
   * @param id the unique ID of the course to update
   * @param version the latest version of the course for O.O.L.
   * @return a boolean indicating success or failure
   */
  override def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        _ <- predicate(existingCourse.version == version)(ServiceError.OfflineLockFail)
        wasDeleted <- lift(courseRepository.delete(existingCourse))
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

  // TODO - to remove
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
   * @param course the course to add users to
   * @param userIds an IndexedSeq of UUID representing the users to be added.
   * @return a boolean indicating success or failure.
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
   * @param course the course to remove users from
   * @param userIds an IndexedSeq of UUID representing the users to be removed.
   * @return boolean indicating success or failure.
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
      user <- lift(authService.find(userId))
      teacher <- lift(authService.find(teacherId))
      userCourses <- lift(courseRepository.list(user, false))
      filteredCourses = userCourses.filter(_.teacherId == teacherId)
      _ <- predicate(filteredCourses.nonEmpty)(RepositoryError.NoResults(s"User ${userId.toString} is not in any courses wiht teacher ${teacherId.toString}"))
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
      _ <- predicate(num > 0 && offset > 0)(ServiceError.BadInput("num, and offset parameters must be positive long integers"))
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
  override def listChats(courseId: UUID, userId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      user <- lift(userRepository.find(userId))
      _ <- predicate(num > 0 && offset > 0)(ServiceError.BadInput("num, and offset parameters must be positive long integers"))
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
        students <- lift(listStudents(course))
        _ <- predicate(course.teacherId == user.id || students.contains(user))(ServiceError.BadPermissions("You must be a member of a course to chat in it."))
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
        existingChat <- lift(findChat(courseId, messageNum))
        newChat = existingChat.copy(hidden = hidden)
        updatedChat <- lift(chatRepository.update(newChat))
      } yield updatedChat
    }
  }
  override def getRandomWord(lang: String): Future[\/[ErrorUnion#Fail, LinkWord]] = {
    transactional { implicit conn =>
      wordRepository.get(lang)
    }
  }

  /**
   * Creating a new link for student registration.
   *
   * @param lang user's language
   * @param courseId course id
   * @return the link for students
   */
  override def createLink(lang: String, courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      for {
        word <- lift(wordRepository.get(lang))
        _ = Logger.error(word.toString)
        link <- lift(linkRepository.create(Link(word.word, courseId, new DateTime())))

      } yield link
    }
  }

  /**
   * Find a link by link name
   *
   * @param link
   * @return
   */
  override def findLink(link: String): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.find(link)
    }
  }

  /**
   * Delete a link based on course id. it will save us some parsing XD
   *
   * @param courseId
   * @return
   */
  override def deleteLink(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.deleteByCourse(courseId)
    }
  }

  /**
   * Find a link by course id
   * @param courseId
   * @return
   */
  override def findLinkByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.findByCourse(courseId)
    }
  }

  override def getCourseLimit(userId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getCourseLimit(userId)
  }

  /**
   * Check if a slug is of the valid format.
   *
   * @param slug the slug to be checked
   * @return a future disjunction containing either the slug, or a failure
   */
  private def isValidSlug(slug: String): Future[\/[ErrorUnion#Fail, String]] = Future successful {
    if ("""[A-Za-z0-9\-]+""".r.unapplySeq(slug).isDefined) \/-(slug)
    else -\/(ServiceError.BadInput(s"$slug is not a valid slug format."))
  }
}
